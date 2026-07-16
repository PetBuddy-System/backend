package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.BookingCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BookingDetailCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BookingUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.BookingResponse;
import com.petbuddy.petbuddystore.dto.response.MediaFileResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.mapper.BookingMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.service.BookingService;
import com.petbuddy.petbuddystore.service.EmailService;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceImpl implements BookingService {
    static final int MAX_PETS_PER_SLOT = 5;
    static final BigDecimal DEPOSIT_RATE = BigDecimal.valueOf(0.2);
    static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    BookingRepository bookingRepository;
    BookingDetailRepository bookingDetailRepository;
    CatalogRepository catalogRepository;
    CatalogTimeSlotRepository catalogTimeSlotRepository;
    PetRepository petRepository;
    StaffScheduleRepository staffScheduleRepository;
    MediaFileRepository mediaFileRepository;
    UserRepository userRepository;
    PaymentService paymentService;
    FileService fileService;
    EmailService emailService;
    BookingMapper bookingMapper;

    @Override
    public BookingResponse createBooking(BookingCreationRequest request) {
        User user = getCurrentUser();
        if (user.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        validateSingleTimeSlot(request);
        LocalDate bookingDate = request.getScheduledAt();
        LocalDateTime scheduledAt = resolveScheduledAt(request, bookingDate);

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode(bookingDate))
                .bookingType(request.getBookingType())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .address(request.getAddress())
                .scheduledAt(scheduledAt)
                .note(request.getNote())
                .bookingStatus(BookingStatus.PENDING_PAYMENT)
                .paymentDeadlineAt(LocalDateTime.now(VIETNAM_ZONE).plusMinutes(15))
                .user(user)
                .build();

        List<BookingDetail> details = new ArrayList<>();
        Map<Integer, Integer> slotReservations = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (BookingDetailCreationRequest detailRequest : request.getBookingDetails()) {
            int reservedInRequest = slotReservations.getOrDefault(detailRequest.getTimeSlotId(), 0);
            BookingDetail detail = buildBookingDetail(detailRequest, booking, user, bookingDate, scheduledAt, reservedInRequest);
            totalAmount = totalAmount.add(detail.getTotalPrice());
            details.add(detail);
            slotReservations.merge(detailRequest.getTimeSlotId(), 1, Integer::sum);
        }

        booking.setBookingDetails(details);
        booking.setTotalAmount(totalAmount);
        booking.setDepositAmount(totalAmount.multiply(DEPOSIT_RATE));
        booking.setRemainingAmount(totalAmount.subtract(booking.getDepositAmount()));

        Booking savedBooking = bookingRepository.save(booking);
        Payment payment = paymentService.createBookingDepositPayment(savedBooking, PaymentMethod.CARD);
        
        BookingResponse response = bookingMapper.toBookingResponse(savedBooking);
        if (payment != null) {
            response.setStripeClientSecret(payment.getStripeClientSecret());
        }
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        User user = getCurrentUser();
        if (user.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return bookingRepository.findByUser_UserIdOrderByCreateAtDesc(user.getUserId())
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @Override
    public PaymentResponse retryPayment(Integer bookingId) {
        User user = getCurrentUser();
        Booking booking = findBooking(bookingId);
        if (user.getRole() != Role.CUSTOMER || !booking.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (booking.getBookingStatus() != BookingStatus.FAILED
                && booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentDeadlineAt(LocalDateTime.now(VIETNAM_ZONE).plusMinutes(15));
        Payment payment = paymentService.createBookingDepositPayment(booking, PaymentMethod.CARD);
        return toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(BookingStatus status, LocalDate fromDate, LocalDate toDate) {
        User user = getCurrentUser();

        if (user.getRole() == Role.STAFF) {
            if (user.getStaffTask() == StaffTask.COORDINATOR) {
                return filterBookings(
                                bookingRepository.findByBookingStatusOrderByCreateAtAsc(BookingStatus.PENDING_ACCEPTANCE),
                                status,
                                fromDate,
                                toDate
                        )
                        .stream()
                        .map(bookingMapper::toBookingResponse)
                        .toList();
            }

            if (user.getStaffTask() == StaffTask.GROOMER) {
                getCurrentWorkingSchedule(user);
                return filterBookings(
                                bookingRepository.findByStaffSchedule_Staff_UserIdOrderByCreateAtDesc(user.getUserId()),
                                status,
                                fromDate,
                                toDate
                        )
                        .stream()
                        .map(bookingMapper::toBookingResponse)
                        .toList();
            }

            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        LocalDateTime from = fromDate == null ? LocalDate.of(1970, 1, 1).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? LocalDate.of(9999, 12, 31).atTime(LocalTime.MAX) : toDate.plusDays(1).atStartOfDay();
        Collection<BookingStatus> statuses = status == null ? Arrays.asList(BookingStatus.values()) : List.of(status);
        return bookingRepository.findByBookingStatusInAndScheduledAtBetweenOrderByCreateAtDesc(statuses, from, to)
                .stream()
                .map(bookingMapper::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(Integer bookingId) {
        User user = getCurrentUser();
        Booking booking = findBooking(bookingId);
        assertCanViewBooking(booking, user);
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    public BookingResponse updateStatus(Integer bookingId, BookingUpdateRequest request) {
        User user = getCurrentUser();
        Booking booking = findBooking(bookingId);
        BookingStatus newStatus = request.getStatus();

        if (newStatus == BookingStatus.ACCEPTED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        if (newStatus == BookingStatus.CANCELLED) {
            cancelBooking(booking, request.getCancelReason(), user);
            return bookingMapper.toBookingResponse(bookingRepository.save(booking));
        }

        assertCanUpdateAssignedBooking(booking, user);
        validateStatusTransition(booking.getBookingStatus(), newStatus);
        validateRequiredBookingMedia(booking, newStatus);
        booking.setBookingStatus(newStatus);
        Booking savedBooking = bookingRepository.save(booking);
        if (newStatus == BookingStatus.READY_FOR_PICKUP) {
            sendBookingEmailAsync(
                    savedBooking,
                    "PetBuddy - Your pet is ready for pickup",
                    "Ready for pickup",
                    "Booking " + savedBooking.getBookingCode() + " is ready for pickup."
            );
        }
        return bookingMapper.toBookingResponse(savedBooking);
    }

    @Override
    public BookingResponse assignGroomer(Integer bookingId, String groomerId) {
        User coordinator = getCurrentUser();
        if (coordinator.getRole() != Role.STAFF || coordinator.getStaffTask() != StaffTask.COORDINATOR) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Booking booking = findBooking(bookingId);
        User groomer = userRepository.findById(groomerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        assignBookingToGroomer(booking, groomer);
        return bookingMapper.toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    public MediaFileResponse uploadBookingMedia(Integer bookingDetailId, BookingMediaType bookingMediaType, MultipartFile file) {
        User user = getCurrentUser();
        BookingDetail detail = bookingDetailRepository.findWithBookingByBookingDetailId(bookingDetailId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_DETAIL_NOT_FOUND));
        assertCanUpdateAssignedBooking(detail.getBooking(), user);
        validateBookingMediaUpload(detail.getBooking(), bookingMediaType);

        MediaFile mediaFile = fileService.uploadBookingImage(file);
        mediaFile.setBookingDetail(detail);
        mediaFile.setBookingMediaType(bookingMediaType);
        return bookingMapper.toMediaFileResponse(mediaFileRepository.save(mediaFile));
    }

    private BookingDetail buildBookingDetail(
            BookingDetailCreationRequest request,
            Booking booking,
            User user,
            LocalDate bookingDate,
            LocalDateTime scheduledAt,
            int reservedInRequest
    ) {
        Catalog catalog = catalogRepository.findById(request.getCatalogId())
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_NOT_FOUND));
        CatalogTimeSlot timeSlot = catalogTimeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_TIME_SLOT_NOT_FOUND));
        PetProfile pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));

        if (!pet.getUser().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.PET_OWNER_INVALID);
        }
        
        if (!booking.getBookingType().equals(catalog.getCatalogType().name())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_TYPE);
        }
        
        if (catalog.getPetSpecies() != null && !catalog.getPetSpecies().equalsIgnoreCase(pet.getSpecies())) {
            throw new AppException(ErrorCode.INVALID_PET_SPECIES);
        }

        if (catalog.getStatus() != CatalogStatus.AVAILABLE) {
            throw new AppException(ErrorCode.CATALOG_NOT_FOUND);
        }
        if (!timeSlot.getCatalog().getCatalogId().equals(catalog.getCatalogId())
                || !Boolean.TRUE.equals(timeSlot.getIsActive())
                || timeSlot.getDayOfWeek() != bookingDate.getDayOfWeek()) {
            throw new AppException(ErrorCode.BOOKING_SLOT_UNAVAILABLE);
        }

        LocalDateTime from = bookingDate.atStartOfDay();
        LocalDateTime to = bookingDate.plusDays(1).atStartOfDay();
        long bookedPets = bookingRepository.countBookedPetsInSlot(
                timeSlot.getId(),
                from,
                to,
                List.of(BookingStatus.FAILED, BookingStatus.CANCELLED)
        );
        int maxPets = timeSlot.getMaxPets() != null ? timeSlot.getMaxPets() : 5;
        if (bookedPets + reservedInRequest >= maxPets) {
            throw new AppException(ErrorCode.BOOKING_SLOT_UNAVAILABLE);
        }

        // Tính phụ thu theo hạng cân thú cưng
        WeightRange petWeightRange = resolvePetWeightRange(catalog, pet);
        BigDecimal surcharge = BigDecimal.ZERO;
        if (catalog.getSurchargeConfig() != null && !catalog.getSurchargeConfig().isBlank()) {
            surcharge = parseSurcharge(catalog.getSurchargeConfig(), petWeightRange);
        }
        int durationMinute = resolveDurationMinute(catalog, petWeightRange);
        validatePetNotAlreadyBooked(pet.getPetId(), scheduledAt, durationMinute);

        BigDecimal unitPrice = catalog.getPrice().add(surcharge);
        return BookingDetail.builder()
                .booking(booking)
                .pet(pet)
                .catalog(catalog)
                .timeSlot(timeSlot)
                .petName(pet.getPetName())
                .petSpecies(pet.getSpecies())
                .petWeight(pet.getWeight() == null ? null : BigDecimal.valueOf(pet.getWeight()))
                .petHealthNote(pet.getHealthNote())
                .catalogName(catalog.getCatalogName())
                .catalogType(catalog.getCatalogType().name())
                .durationMinute(durationMinute)
                .unitPrice(unitPrice)
                .quantity(1)
                .totalPrice(unitPrice)
                .note(request.getNote())
                .build();
    }

    private void assignBookingToGroomer(Booking booking, User groomer) {
        if (groomer.getRole() != Role.STAFF || groomer.getStaffTask() != StaffTask.GROOMER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_ACCEPTANCE) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        StaffSchedule staffSchedule = getScheduleForBookingDate(groomer, booking.getScheduledAt().toLocalDate());
        validateStaffNoOverlap(staffSchedule, booking);

        int bookingDuration = getBookingDuration(booking);
        int assignedDuration = bookingRepository.findByStaffScheduleAndBookingStatusIn(
                        staffSchedule,
                        List.of(BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS, BookingStatus.READY_FOR_PICKUP)
                )
                .stream()
                .mapToInt(this::getBookingDuration)
                .sum();
        int shiftDuration = (int) Duration.between(
                staffSchedule.getWorkSchedule().getStartTime(),
                staffSchedule.getWorkSchedule().getEndTime()
        ).toMinutes();

        // Chỉ nhận đơn khi ca hiện tại còn đủ thời lượng thực hiện toàn bộ dịch vụ.
        if (shiftDuration - assignedDuration < bookingDuration) {
            throw new AppException(ErrorCode.STAFF_WORK_TIME_NOT_ENOUGH);
        }

        booking.setStaffSchedule(staffSchedule);
        booking.setBookingStatus(BookingStatus.ACCEPTED);
    }

    private void cancelBooking(Booking booking, String cancelReason, User user) {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new AppException(ErrorCode.CANCEL_REASON_REQUIRED);
        }
        if (user.getRole() == Role.CUSTOMER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (user.getRole() == Role.STAFF) {
            if (user.getStaffTask() == StaffTask.COORDINATOR && booking.getBookingStatus() == BookingStatus.PENDING_ACCEPTANCE) {
                getCurrentWorkingSchedule(user);
            } else {
                assertCanUpdateAssignedBooking(booking, user);
            }
        }
        validateBookingCanBeCancelled(booking);
        booking.setCancelReason(cancelReason);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.getPayments().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .forEach(payment -> payment.setStatus(PaymentStatus.REFUNDED));
        sendBookingEmailAsync(
                booking,
                "PetBuddy - Booking cancelled",
                "Cancelled",
                "Booking " + booking.getBookingCode() + " has been cancelled. Reason: " + cancelReason
        );
    }

    private void validateStatusTransition(BookingStatus currentStatus, BookingStatus newStatus) {
        Map<BookingStatus, List<BookingStatus>> transitions = Map.of(
                BookingStatus.ACCEPTED, List.of(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED),
                BookingStatus.IN_PROGRESS, List.of(BookingStatus.READY_FOR_PICKUP, BookingStatus.CANCELLED),
                BookingStatus.READY_FOR_PICKUP, List.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED)
        );

        if (!transitions.getOrDefault(currentStatus, List.of()).contains(newStatus)) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
    }

    private void validateBookingMediaUpload(Booking booking, BookingMediaType bookingMediaType) {
        if (bookingMediaType == BookingMediaType.BEFORE_SERVICE
                && booking.getBookingStatus() != BookingStatus.ACCEPTED) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        if (bookingMediaType == BookingMediaType.AFTER_SERVICE
                && booking.getBookingStatus() != BookingStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
    }

    private void validateRequiredBookingMedia(Booking booking, BookingStatus newStatus) {
        if (newStatus == BookingStatus.IN_PROGRESS
                && !hasBookingMedia(booking.getBookingId(), BookingMediaType.BEFORE_SERVICE)) {
            throw new AppException(ErrorCode.BOOKING_BEFORE_SERVICE_MEDIA_REQUIRED);
        }
        if (newStatus == BookingStatus.READY_FOR_PICKUP
                && !hasBookingMedia(booking.getBookingId(), BookingMediaType.AFTER_SERVICE)) {
            throw new AppException(ErrorCode.BOOKING_AFTER_SERVICE_MEDIA_REQUIRED);
        }
    }

    private boolean hasBookingMedia(Integer bookingId, BookingMediaType bookingMediaType) {
        return mediaFileRepository.existsByBookingDetail_Booking_BookingIdAndBookingMediaTypeAndMediaStatus(
                bookingId,
                bookingMediaType,
                MediaStatus.ACTIVE
        );
    }

    private void assertCanViewBooking(Booking booking, User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) {
            return;
        }
        if (user.getRole() == Role.CUSTOMER && booking.getUser().getUserId().equals(user.getUserId())) {
            return;
        }
        if (user.getRole() == Role.STAFF && user.getStaffTask() == StaffTask.COORDINATOR
                && booking.getBookingStatus() == BookingStatus.PENDING_ACCEPTANCE) {
            return;
        }
        if (user.getRole() == Role.STAFF && user.getStaffTask() == StaffTask.GROOMER && isAssignedToStaff(booking, user)) {
            return;
        }
        throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    private void assertCanUpdateAssignedBooking(Booking booking, User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) {
            return;
        }
        if (user.getRole() == Role.STAFF && user.getStaffTask() == StaffTask.GROOMER && isAssignedToStaff(booking, user)) {
            return;
        }
        throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    private boolean isAssignedToStaff(Booking booking, User user) {
        return booking.getStaffSchedule() != null
                && booking.getStaffSchedule().getStaff().getUserId().equals(user.getUserId());
    }

    private StaffSchedule getCurrentWorkingSchedule(User staff) {
        return staffScheduleRepository
                .findWorkingSchedule(staff.getUserId(), LocalDate.now(), ScheduleStatus.WORKING)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_CHECKED_IN));
    }

    private StaffSchedule getScheduleForBookingDate(User staff, LocalDate bookingDate) {
        return staffScheduleRepository
                .findScheduleForDate(
                        staff.getUserId(),
                        bookingDate,
                        List.of(ScheduleStatus.WORKING)
                )
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_CHECKED_IN));
    }

    private LocalDateTime resolveScheduledAt(BookingCreationRequest request, LocalDate bookingDate) {
        Integer firstTimeSlotId = request.getBookingDetails().getFirst().getTimeSlotId();
        CatalogTimeSlot firstTimeSlot = catalogTimeSlotRepository.findById(firstTimeSlotId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_TIME_SLOT_NOT_FOUND));

        LocalDateTime scheduledAt = LocalDateTime.of(bookingDate, firstTimeSlot.getStartTime());
        if (scheduledAt.isBefore(LocalDateTime.now(VIETNAM_ZONE))) {
            throw new AppException(ErrorCode.BOOKING_TIME_INVALID);
        }
        return scheduledAt;
    }

    private void validateSingleTimeSlot(BookingCreationRequest request) {
        long timeSlotCount = request.getBookingDetails().stream()
                .map(BookingDetailCreationRequest::getTimeSlotId)
                .distinct()
                .count();

        if (timeSlotCount > 1) {
            throw new AppException(ErrorCode.ONLY_ONE_TIMESLOT_PER_BOOKING);
        }
    }

    private void validatePetNotAlreadyBooked(String petId, LocalDateTime scheduledAt, int newDuration) {
        LocalDateTime startOfDay = scheduledAt.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<BookingDetail> existingBookings = bookingDetailRepository.findActiveBookingsForPetOnDay(
                petId,
                startOfDay,
                endOfDay,
                List.of(BookingStatus.FAILED, BookingStatus.CANCELLED, BookingStatus.COMPLETED)
        );

        LocalDateTime newStart = scheduledAt;
        LocalDateTime newEnd = newStart.plusMinutes(newDuration);

        for (BookingDetail existing : existingBookings) {
            LocalDateTime oldStart = existing.getBooking().getScheduledAt();
            LocalDateTime oldEnd = oldStart.plusMinutes(existing.getDurationMinute() != null ? existing.getDurationMinute() : 0);

            if (newStart.isBefore(oldEnd) && newEnd.isAfter(oldStart)) {
                throw new AppException(ErrorCode.PET_ALREADY_BOOKED);
            }
        }
    }

    private void validateStaffNoOverlap(StaffSchedule staffSchedule, Booking booking) {
        boolean hasOverlap = bookingRepository.existsByStaffScheduleAndScheduledAtAndBookingStatusIn(
                staffSchedule,
                booking.getScheduledAt(),
                List.of(BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS, BookingStatus.READY_FOR_PICKUP)
        );

        if (hasOverlap) {
            throw new AppException(ErrorCode.STAFF_OVERLAP_BOOKING);
        }
    }

    private void validateBookingCanBeCancelled(Booking booking) {
        if (EnumSet.of(
                BookingStatus.IN_PROGRESS,
                BookingStatus.READY_FOR_PICKUP,
                BookingStatus.COMPLETED,
                BookingStatus.CANCELLED,
                BookingStatus.FAILED
        ).contains(booking.getBookingStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
    }

    private void sendBookingEmailAsync(Booking booking, String subject, String bookingStatus, String messageBody) {
        if (booking.getUser() == null || booking.getUser().getEmail() == null) {
            return;
        }
        CompletableFuture.runAsync(() -> emailService.sendBookingNotification(
                booking.getUser().getEmail(),
                subject,
                booking.getCustomerName(),
                booking.getBookingCode(),
                bookingStatus,
                messageBody
        ));
    }

    private int getBookingDuration(Booking booking) {
        return booking.getBookingDetails().stream()
                .mapToInt(detail -> detail.getDurationMinute() == null ? 0 : detail.getDurationMinute())
                .sum();
    }

    private List<Booking> filterBookings(
            List<Booking> bookings,
            BookingStatus status,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return bookings.stream()
                .filter(booking -> status == null || booking.getBookingStatus() == status)
                .filter(booking -> fromDate == null || !booking.getScheduledAt().toLocalDate().isBefore(fromDate))
                .filter(booking -> toDate == null || !booking.getScheduledAt().toLocalDate().isAfter(toDate))
                .toList();
    }

    private Booking findBooking(Integer bookingId) {
        return bookingRepository.findWithDetailsByBookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private String generateBookingCode(LocalDate bookingDate) {
        return "BK-" + bookingDate.toString().replace("-", "").substring(2)
                + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .bookingId(payment.getBooking() == null ? null : payment.getBooking().getBookingId())
                .bookingCode(payment.getBooking() == null ? null : payment.getBooking().getBookingCode())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .stripeClientSecret(payment.getStripeClientSecret())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private BigDecimal parseSurcharge(String surchargeConfig, WeightRange weightRange) {
        for (String part : surchargeConfig.split(";")) {
            String[] kv = part.split(":");
            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(weightRange.name())) {
                return new BigDecimal(kv[1].trim());
            }
        }
        return BigDecimal.ZERO;
    }

    private WeightRange resolvePetWeightRange(Catalog catalog, PetProfile pet) {
        boolean requiresWeight = catalog.getSurchargeConfig() != null && !catalog.getSurchargeConfig().isBlank()
                || catalog.getDurationConfig() != null && !catalog.getDurationConfig().isBlank();
        if (!requiresWeight) {
            return null;
        }
        if (pet.getWeight() == null) {
            throw new AppException(ErrorCode.PET_WEIGHT_REQUIRED);
        }
        return WeightRange.fromWeight(pet.getWeight());
    }

    private int resolveDurationMinute(Catalog catalog, WeightRange weightRange) {
        if (catalog.getDurationConfig() != null && !catalog.getDurationConfig().isBlank() && weightRange != null) {
            Integer configuredDuration = parseDuration(catalog.getDurationConfig(), weightRange);
            if (configuredDuration != null) {
                return configuredDuration;
            }
        }
        return catalog.getDurationMinute() != null ? catalog.getDurationMinute() : 0;
    }

    private Integer parseDuration(String durationConfig, WeightRange weightRange) {
        for (String part : durationConfig.split(";")) {
            String[] kv = part.split(":");
            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(weightRange.name())) {
                return Integer.parseInt(kv[1].trim());
            }
        }
        return null;
    }
}
