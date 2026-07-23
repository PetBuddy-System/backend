package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.AvailableGroomerRequest;
import com.petbuddy.petbuddystore.dto.request.BookingCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BookingDetailCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BookingUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.AvailableGroomerResponse;
import com.petbuddy.petbuddystore.dto.response.BookingResponse;
import com.petbuddy.petbuddystore.dto.response.BookingPreviewResponse;
import com.petbuddy.petbuddystore.dto.response.MediaFileResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.dto.response.ShippingFeeResponse;
import com.petbuddy.petbuddystore.mapper.BookingMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.service.BookingService;
import com.petbuddy.petbuddystore.service.BookingAssignmentService;
import com.petbuddy.petbuddystore.service.EmailService;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.PaymentService;
import com.petbuddy.petbuddystore.service.ShippingRuleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceImpl implements BookingService {
    static final int MAX_PETS_PER_SLOT = 5;
    static final BigDecimal DEPOSIT_RATE = BigDecimal.valueOf(0.2);
    static final int DEFAULT_HOME_TRAVEL_MINUTE = 30;
    static final double DEFAULT_TRAVEL_SPEED_KMH = 25.0;
    static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final List<BookingStatus> STAFF_OCCUPYING_STATUSES = List.of(
            BookingStatus.ACCEPTED,
            BookingStatus.ON_THE_WAY,
            BookingStatus.IN_PROGRESS,
            BookingStatus.READY_FOR_PICKUP
    );
    static final List<ScheduleStatus> ASSIGNABLE_SCHEDULE_STATUSES = List.of(
            ScheduleStatus.SCHEDULED,
            ScheduleStatus.WORKING
    );

    BookingRepository bookingRepository;
    BookingDetailRepository bookingDetailRepository;
    CatalogRepository catalogRepository;
    CatalogTimeSlotRepository catalogTimeSlotRepository;
    PetRepository petRepository;
    StaffScheduleRepository staffScheduleRepository;
    MediaFileRepository mediaFileRepository;
    UserRepository userRepository;
    PaymentService paymentService;
    BookingAssignmentService bookingAssignmentService;
    ShippingRuleService shippingRuleService;
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
        validateNoDuplicatePets(request);
        validateHomeBookingRequest(request);
        LocalDate bookingDate = request.getScheduledAt();
        LocalDateTime scheduledAt = resolveScheduledAt(request, bookingDate);
        StaffAssignmentMode assignmentMode = resolveAssignmentMode(request.getAssignmentMode());
        HomeFeeSnapshot homeFeeSnapshot = resolveHomeFeeSnapshot(request);

        Booking booking = Booking.builder()
                .bookingCode(generateBookingCode(bookingDate))
                .bookingType(request.getBookingType())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .addressNote(request.getAddressNote())
                .distanceKm(homeFeeSnapshot.distanceKm())
                .travelFee(homeFeeSnapshot.travelFee())
                .estimatedTravelMinute(homeFeeSnapshot.estimatedTravelMinute())
                .homeServiceRequirementsAccepted(request.getHomeServiceRequirementsAccepted())
                .scheduledAt(scheduledAt)
                .note(request.getNote())
                .bookingStatus(BookingStatus.PENDING_PAYMENT)
                .assignmentMode(assignmentMode)
                .requestedStaffId(assignmentMode == StaffAssignmentMode.SELECTED ? request.getRequestedStaffId() : null)
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
        booking.setTotalAmount(totalAmount.add(homeFeeSnapshot.travelFee()));
        booking.setDepositAmount(booking.getTotalAmount().multiply(DEPOSIT_RATE));
        booking.setRemainingAmount(booking.getTotalAmount().subtract(booking.getDepositAmount()));
        if (assignmentMode == StaffAssignmentMode.SELECTED) {
            bookingAssignmentService.validateSelectedGroomer(booking, request.getRequestedStaffId());
        }

        Booking savedBooking = bookingRepository.save(booking);
        Payment payment = paymentService.createBookingDepositPayment(savedBooking, PaymentMethod.CARD);
        
        BookingResponse response = toBookingResponse(savedBooking);
        if (payment != null) {
            response.setStripeClientSecret(payment.getStripeClientSecret());
        }
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingPreviewResponse previewBooking(BookingCreationRequest request) {
        User user = getCurrentUser();
        if (user.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        validateSingleTimeSlot(request);
        validateNoDuplicatePets(request);
        validateHomeBookingRequest(request);
        LocalDate bookingDate = request.getScheduledAt();
        LocalDateTime scheduledAt = resolveScheduledAt(request, bookingDate);
        HomeFeeSnapshot homeFeeSnapshot = resolveHomeFeeSnapshot(request);

        BigDecimal serviceAmount = BigDecimal.ZERO;
        BigDecimal surchargeAmount = BigDecimal.ZERO;
        int estimatedServiceMinute = 0;
        Map<Integer, Integer> slotReservations = new HashMap<>();

        Booking previewBooking = Booking.builder()
                .bookingType(request.getBookingType())
                .scheduledAt(scheduledAt)
                .build();

        for (BookingDetailCreationRequest detailRequest : request.getBookingDetails()) {
            int reservedInRequest = slotReservations.getOrDefault(detailRequest.getTimeSlotId(), 0);
            BookingDetail detail = buildBookingDetail(detailRequest, previewBooking, user, bookingDate, scheduledAt, reservedInRequest);
            serviceAmount = serviceAmount.add(detail.getBasePrice() == null ? BigDecimal.ZERO : detail.getBasePrice());
            surchargeAmount = surchargeAmount.add(detail.getAdditionalPrice() == null ? BigDecimal.ZERO : detail.getAdditionalPrice());
            estimatedServiceMinute += getDetailDuration(detail);
            slotReservations.merge(detailRequest.getTimeSlotId(), 1, Integer::sum);
        }

        BigDecimal totalAmount = serviceAmount.add(surchargeAmount).add(homeFeeSnapshot.travelFee());
        return BookingPreviewResponse.builder()
                .serviceAmount(serviceAmount)
                .surchargeAmount(surchargeAmount)
                .travelFee(homeFeeSnapshot.travelFee())
                .distanceKm(homeFeeSnapshot.distanceKm())
                .estimatedTravelMinute(homeFeeSnapshot.estimatedTravelMinute())
                .estimatedServiceMinute(estimatedServiceMinute)
                .totalAmount(totalAmount)
                .depositAmount(totalAmount.multiply(DEPOSIT_RATE))
                .build();
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
                .map(this::toBookingResponse)
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
                                bookingRepository.findByBookingStatusInAndScheduledAtBetweenOrderByCreateAtDesc(
                                        List.of(
                                                BookingStatus.WAITING_STAFF,
                                                BookingStatus.ACCEPTED,
                                                BookingStatus.ON_THE_WAY,
                                                BookingStatus.IN_PROGRESS,
                                                BookingStatus.READY_FOR_PICKUP
                                        ),
                                        LocalDate.of(1970, 1, 1).atStartOfDay(),
                                        LocalDate.of(9999, 12, 31).atTime(LocalTime.MAX)
                                ),
                                status,
                                fromDate,
                                toDate
                        )
                        .stream()
                        .map(this::toBookingResponse)
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
                        .map(this::toBookingResponse)
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
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(Integer bookingId) {
        User user = getCurrentUser();
        Booking booking = findBooking(bookingId);
        assertCanViewBooking(booking, user);
        return toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableGroomerResponse> getAvailableGroomers(AvailableGroomerRequest request) {
        User user = getCurrentUser();
        boolean canViewAvailableGroomers = user.getRole() == Role.CUSTOMER
                || user.getRole() == Role.MANAGER
                || user.getRole() == Role.ADMIN
                || (user.getRole() == Role.STAFF && user.getStaffTask() == StaffTask.COORDINATOR);
        if (!canViewAvailableGroomers) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        int bookingDuration = calculateBookingDurationForRequest(request.getBookingDetails(), user);
        boolean homeBooking = isHomeBookingForAvailableGroomers(request);
        int travelMinute = homeBooking ? resolveTravelMinuteForAvailableGroomers(request) : 0;
        LocalDateTime serviceStart = request.getScheduledAt();
        LocalDateTime bookingStart = serviceStart.minusMinutes(travelMinute);
        LocalDateTime bookingEnd = serviceStart.plusMinutes(bookingDuration).plusMinutes(travelMinute);

        return staffScheduleRepository.findGroomerSchedulesForAssignment(
                        serviceStart.toLocalDate(),
                        ASSIGNABLE_SCHEDULE_STATUSES,
                        StaffTask.GROOMER,
                        UserStatus.ACTIVE
                )
                .stream()
                .filter(schedule -> isScheduleAssignable(schedule, bookingStart, bookingEnd, null))
                .map(this::toAvailableGroomerResponse)
                .toList();
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
            return toBookingResponse(bookingRepository.save(booking));
        }

        assertCanUpdateAssignedBooking(booking, user);
        validateStatusTransition(booking, newStatus);
        validateRequiredBookingMedia(booking, newStatus);
        if (newStatus == BookingStatus.IN_PROGRESS) {
            validateAssignedStaffCheckedIn(booking);
            if (isHomeBooking(booking)) {
                validateNoOtherInProgressBooking(booking);
            }
        }
        if (newStatus == BookingStatus.ON_THE_WAY) {
            validateAssignedStaffCheckedIn(booking);
            validateHomeDepartureTime(booking);
            booking.setDepartedAt(LocalDateTime.now(VIETNAM_ZONE));
        }
        if (newStatus == BookingStatus.IN_PROGRESS) {
            booking.setActualStartedAt(LocalDateTime.now(VIETNAM_ZONE));
        }
        if (newStatus == BookingStatus.COMPLETED) {
            booking.setActualCompletedAt(LocalDateTime.now(VIETNAM_ZONE));
        }
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
        return toBookingResponse(savedBooking);
    }

    @Override
    public BookingResponse assignGroomer(Integer bookingId, String groomerId) {
        User currentUser = getCurrentUser();
        boolean canReassign = currentUser.getRole() == Role.MANAGER
                || (currentUser.getRole() == Role.STAFF && currentUser.getStaffTask() == StaffTask.COORDINATOR);
        if (!canReassign) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Booking booking = findBooking(bookingId);
        if (currentUser.getRole() == Role.STAFF
                && currentUser.getStaffTask() == StaffTask.COORDINATOR
                && booking.getBookingStatus() != BookingStatus.WAITING_STAFF) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        bookingAssignmentService.assignManual(booking, groomerId);
        return toBookingResponse(bookingRepository.save(booking));
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

    @Override
    public void autoReassignOverdueBookings() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime reassignWindowEnd = now.plusMinutes(15);
        List<Booking> candidates = bookingRepository.findAutoReassignCandidatesForUpdate(
                now.minusHours(1),
                reassignWindowEnd,
                List.of(BookingStatus.ACCEPTED)
        );

        for (Booking booking : candidates) {
            try {
                StaffSchedule oldSchedule = booking.getStaffSchedule();
                if (oldSchedule == null || oldSchedule.getScheduleStatus() == ScheduleStatus.WORKING) {
                    continue;
                }
                if (booking.getLastAutoReassignAt() != null
                        && booking.getLastAutoReassignAt().isAfter(now.minusMinutes(10))) {
                    continue;
                }

                String oldStaffId = oldSchedule.getStaff().getUserId();

                if (booking.getAssignmentMode() == StaffAssignmentMode.SELECTED) {
                    booking.setStaffSchedule(null);
                    booking.setBookingStatus(BookingStatus.WAITING_STAFF);
                    booking.setLastAutoReassignAt(now);
                    bookingRepository.save(booking);
                    log.warn("Booking {} moved to WAITING_STAFF because selected staff {} has not checked in",
                            booking.getBookingId(), oldStaffId);
                    continue;
                }

                LocalDateTime bookingStart = getOccupiedStart(booking);
                LocalDateTime bookingEnd = getOccupiedEnd(booking);
                StaffSchedule newSchedule = bookingAssignmentService.findBestAvailableGroomer(bookingStart, bookingEnd, oldStaffId);

                if (newSchedule == null) {
                    booking.setStaffSchedule(null);
                    booking.setBookingStatus(BookingStatus.WAITING_STAFF);
                    booking.setLastAutoReassignAt(now);
                    bookingRepository.save(booking);
                    log.warn("Booking {} moved to WAITING_STAFF because staff {} has not checked in",
                            booking.getBookingId(), oldStaffId);
                    continue;
                }

                booking.setStaffSchedule(newSchedule);
                newSchedule.setAssignedAt(now);
                staffScheduleRepository.save(newSchedule);
                booking.setLastAutoReassignAt(now);
                bookingRepository.save(booking);
                log.info("Booking {} auto reassigned from staff {} to staff {} because old staff has not checked in",
                        booking.getBookingId(), oldStaffId, newSchedule.getStaff().getUserId());
            } catch (Exception ex) {
                log.error("Failed to auto reassign booking {}: {}", booking.getBookingId(), ex.getMessage(), ex);
            }
        }
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
        PricingSnapshot pricing = calculatePricingSnapshot(catalog, pet);
        validatePetNotAlreadyBooked(pet.getPetId(), scheduledAt, pricing.totalDurationMinute());

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
                .durationMinute(pricing.totalDurationMinute())
                .baseDurationMinute(pricing.baseDurationMinute())
                .additionalDurationMinute(pricing.additionalDurationMinute())
                .totalDurationMinute(pricing.totalDurationMinute())
                .unitPrice(pricing.totalPrice())
                .basePrice(pricing.basePrice())
                .additionalPrice(pricing.additionalPrice())
                .weightRange(pricing.weightRange())
                .quantity(1)
                .totalPrice(pricing.totalPrice())
                .note(request.getNote())
                .build();
    }


    private void cancelBooking(Booking booking, String cancelReason, User user) {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new AppException(ErrorCode.CANCEL_REASON_REQUIRED);
        }
        if (user.getRole() == Role.CUSTOMER) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (user.getRole() == Role.STAFF) {
            if (user.getStaffTask() == StaffTask.COORDINATOR && canCoordinatorManageBooking(booking)) {
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

    private void validateStatusTransition(Booking booking, BookingStatus newStatus) {
        Map<BookingStatus, List<BookingStatus>> transitions = isHomeBooking(booking)
                ? Map.of(
                        BookingStatus.ACCEPTED, List.of(BookingStatus.ON_THE_WAY, BookingStatus.CANCELLED),
                        BookingStatus.ON_THE_WAY, List.of(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED),
                        BookingStatus.IN_PROGRESS, List.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED)
                )
                : Map.of(
                        BookingStatus.ACCEPTED, List.of(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED),
                        BookingStatus.IN_PROGRESS, List.of(BookingStatus.READY_FOR_PICKUP, BookingStatus.CANCELLED),
                        BookingStatus.READY_FOR_PICKUP, List.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED)
                );

        if (isHomeBooking(booking) && newStatus == BookingStatus.READY_FOR_PICKUP) {
            throw new AppException(ErrorCode.HOME_BOOKING_CANNOT_READY_FOR_PICKUP);
        }
        if (!transitions.getOrDefault(booking.getBookingStatus(), List.of()).contains(newStatus)) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
    }

    private void validateBookingMediaUpload(Booking booking, BookingMediaType bookingMediaType) {
        BookingStatus expectedBeforeStatus = isHomeBooking(booking) ? BookingStatus.ON_THE_WAY : BookingStatus.ACCEPTED;
        if (bookingMediaType == BookingMediaType.BEFORE_SERVICE
                && booking.getBookingStatus() != expectedBeforeStatus) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        if (bookingMediaType == BookingMediaType.AFTER_SERVICE
                && booking.getBookingStatus() != BookingStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
    }

    private void validateRequiredBookingMedia(Booking booking, BookingStatus newStatus) {
        if (newStatus == BookingStatus.IN_PROGRESS
                && !hasBookingMediaForEveryDetail(booking, BookingMediaType.BEFORE_SERVICE)) {
            throw new AppException(ErrorCode.BOOKING_BEFORE_SERVICE_MEDIA_REQUIRED);
        }
        if ((newStatus == BookingStatus.READY_FOR_PICKUP || newStatus == BookingStatus.COMPLETED)
                && !hasBookingMediaForEveryDetail(booking, BookingMediaType.AFTER_SERVICE)) {
            throw new AppException(ErrorCode.BOOKING_AFTER_SERVICE_MEDIA_REQUIRED);
        }
    }

    private boolean hasBookingMediaForEveryDetail(Booking booking, BookingMediaType bookingMediaType) {
        return booking.getBookingDetails().stream()
                .allMatch(detail -> mediaFileRepository.existsByBookingDetail_BookingDetailIdAndBookingMediaTypeAndMediaStatus(
                        detail.getBookingDetailId(),
                        bookingMediaType,
                        MediaStatus.ACTIVE
                ));
    }

    private void assertCanViewBooking(Booking booking, User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER) {
            return;
        }
        if (user.getRole() == Role.CUSTOMER && booking.getUser().getUserId().equals(user.getUserId())) {
            return;
        }
        if (user.getRole() == Role.STAFF && user.getStaffTask() == StaffTask.COORDINATOR
                && canCoordinatorManageBooking(booking)) {
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

    private boolean canCoordinatorManageBooking(Booking booking) {
        return EnumSet.of(
                BookingStatus.WAITING_STAFF,
                BookingStatus.ACCEPTED,
                BookingStatus.ON_THE_WAY,
                BookingStatus.IN_PROGRESS,
                BookingStatus.READY_FOR_PICKUP
        ).contains(booking.getBookingStatus());
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

    private void validateNoDuplicatePets(BookingCreationRequest request) {
        long petCount = request.getBookingDetails().stream()
                .map(BookingDetailCreationRequest::getPetId)
                .distinct()
                .count();
        if (petCount != request.getBookingDetails().size()) {
            throw new AppException(ErrorCode.PET_BOOKING_OVERLAP);
        }
    }

    private void validateHomeBookingRequest(BookingCreationRequest request) {
        if (!isHomeBooking(request.getBookingType())) {
            return;
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new AppException(ErrorCode.HOME_ADDRESS_REQUIRED);
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new AppException(ErrorCode.HOME_LOCATION_REQUIRED);
        }
        if (request.getLatitude() < -90 || request.getLatitude() > 90
                || request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new AppException(ErrorCode.INVALID_HOME_LOCATION);
        }
        if (!Boolean.TRUE.equals(request.getHomeServiceRequirementsAccepted())) {
            throw new AppException(ErrorCode.HOME_REQUIREMENTS_NOT_ACCEPTED);
        }

        long catalogCount = request.getBookingDetails().stream()
                .map(BookingDetailCreationRequest::getCatalogId)
                .distinct()
                .count();
        if (catalogCount > 1) {
            throw new AppException(ErrorCode.INVALID_BOOKING_TYPE);
        }
    }

    private HomeFeeSnapshot resolveHomeFeeSnapshot(BookingCreationRequest request) {
        if (!isHomeBooking(request.getBookingType())) {
            return new HomeFeeSnapshot(null, BigDecimal.ZERO, 0);
        }
        try {
            ShippingFeeResponse shippingFee = shippingRuleService.calculateFee(request.getLatitude(), request.getLongitude());
            return new HomeFeeSnapshot(
                    shippingFee.getDistanceKm(),
                    shippingFee.getShippingFee() == null ? BigDecimal.ZERO : shippingFee.getShippingFee(),
                    estimateTravelMinute(shippingFee.getDistanceKm())
            );
        } catch (AppException ex) {
            if (ex.getErrorCode() == ErrorCode.LOCATION_OUTSIDE_HCM
                    || ex.getErrorCode() == ErrorCode.LOCATION_ON_WATER
                    || ex.getErrorCode() == ErrorCode.SHIPPING_CONFIG_NOT_FOUND) {
                throw new AppException(ErrorCode.HOME_ADDRESS_OUT_OF_SERVICE_AREA);
            }
            if (ex.getErrorCode() == ErrorCode.INVALID_COORDINATES) {
                throw new AppException(ErrorCode.INVALID_HOME_LOCATION);
            }
            throw ex;
        }
    }

    private int estimateTravelMinute(Double distanceKm) {
        if (distanceKm == null || distanceKm <= 0) {
            return DEFAULT_HOME_TRAVEL_MINUTE;
        }
        return Math.max(DEFAULT_HOME_TRAVEL_MINUTE,
                (int) Math.ceil(distanceKm / DEFAULT_TRAVEL_SPEED_KMH * 60));
    }

    private boolean isHomeBookingForAvailableGroomers(AvailableGroomerRequest request) {
        if (request.getBookingType() == null || request.getBookingType().isBlank()) {
            return false;
        }
        return isHomeBooking(request.getBookingType());
    }

    private int resolveTravelMinuteForAvailableGroomers(AvailableGroomerRequest request) {
        if (request.getEstimatedTravelMinute() != null && request.getEstimatedTravelMinute() > 0) {
            return request.getEstimatedTravelMinute();
        }
        if (request.getLatitude() != null && request.getLongitude() != null) {
            ShippingFeeResponse shippingFee = shippingRuleService.calculateFee(request.getLatitude(), request.getLongitude());
            return estimateTravelMinute(shippingFee.getDistanceKm());
        }
        return DEFAULT_HOME_TRAVEL_MINUTE;
    }

    private boolean isHomeBooking(String bookingType) {
        return resolveBookingType(bookingType) == LocationType.AT_HOME;
    }

    private boolean isHomeBooking(Booking booking) {
        return isHomeBooking(booking.getBookingType());
    }

    private LocationType resolveBookingType(String bookingType) {
        try {
            return LocationType.valueOf(bookingType);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_BOOKING_TYPE);
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
            LocalDateTime oldEnd = oldStart.plusMinutes(getDetailDuration(existing));

            if (newStart.isBefore(oldEnd) && newEnd.isAfter(oldStart)) {
                throw new AppException(ErrorCode.PET_BOOKING_OVERLAP);
            }
        }
    }

    private void validateStaffNoOverlap(StaffSchedule staffSchedule, Booking booking) {
        LocalDateTime bookingStart = booking.getScheduledAt();
        LocalDateTime bookingEnd = bookingStart.plusMinutes(getBookingDuration(booking));
        boolean hasOverlap = hasStaffOverlap(staffSchedule, bookingStart, bookingEnd, booking.getBookingId());

        if (hasOverlap) {
            throw new AppException(ErrorCode.STAFF_BOOKING_OVERLAP);
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
                .mapToInt(this::getDetailDuration)
                .sum();
    }

    private int getDetailDuration(BookingDetail detail) {
        if (detail.getTotalDurationMinute() != null) {
            return detail.getTotalDurationMinute();
        }
        return detail.getDurationMinute() == null ? 0 : detail.getDurationMinute();
    }

    private LocalDateTime getOccupiedStart(Booking booking) {
        if (!isHomeBooking(booking)) {
            return booking.getScheduledAt();
        }
        return booking.getScheduledAt().minusMinutes(getEstimatedTravelMinute(booking));
    }

    private LocalDateTime getOccupiedEnd(Booking booking) {
        LocalDateTime serviceEnd = booking.getScheduledAt().plusMinutes(getBookingDuration(booking));
        if (!isHomeBooking(booking)) {
            return serviceEnd;
        }
        return serviceEnd.plusMinutes(getEstimatedTravelMinute(booking));
    }

    private int getEstimatedTravelMinute(Booking booking) {
        return booking.getEstimatedTravelMinute() == null || booking.getEstimatedTravelMinute() <= 0
                ? DEFAULT_HOME_TRAVEL_MINUTE
                : booking.getEstimatedTravelMinute();
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

    private BookingResponse toBookingResponse(Booking booking) {
        BookingResponse response = bookingMapper.toBookingResponse(booking);
        if (booking.getRequestedStaffId() != null && !booking.getRequestedStaffId().isBlank()) {
            userRepository.findById(booking.getRequestedStaffId())
                    .map(User::getFullName)
                    .ifPresent(response::setRequestedStaffName);
        }
        return response;
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

    private StaffAssignmentMode resolveAssignmentMode(StaffAssignmentMode assignmentMode) {
        return assignmentMode == null ? StaffAssignmentMode.AUTO : assignmentMode;
    }




    private boolean isScheduleAssignable(
            StaffSchedule schedule,
            LocalDateTime bookingStart,
            LocalDateTime bookingEnd,
            Integer ignoredBookingId
    ) {
        if (schedule == null || schedule.getWorkSchedule() == null || schedule.getStaff() == null) {
            return false;
        }
        if (schedule.getScheduleStatus() == ScheduleStatus.CANCELLED
                || schedule.getAttendanceStatus() == AttendanceStatus.LEAVE
                || schedule.getAttendanceStatus() == AttendanceStatus.ABSENT) {
            return false;
        }
        LocalDateTime shiftStart = LocalDateTime.of(
                schedule.getWorkSchedule().getWorkDate(),
                schedule.getWorkSchedule().getStartTime()
        );
        LocalDateTime shiftEnd = LocalDateTime.of(
                schedule.getWorkSchedule().getWorkDate(),
                schedule.getWorkSchedule().getEndTime()
        );
        if (bookingStart.isBefore(shiftStart) || bookingEnd.isAfter(shiftEnd)) {
            return false;
        }
        return !hasStaffOverlap(schedule, bookingStart, bookingEnd, ignoredBookingId);
    }

    private boolean hasStaffOverlap(
            StaffSchedule schedule,
            LocalDateTime bookingStart,
            LocalDateTime bookingEnd,
            Integer ignoredBookingId
    ) {
        LocalDateTime startOfDay = bookingStart.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return bookingRepository.findByStaffScheduleAndBookingStatusInAndScheduledAtBetween(
                        schedule,
                        STAFF_OCCUPYING_STATUSES,
                        startOfDay,
                        endOfDay
                )
                .stream()
                .filter(existing -> ignoredBookingId == null || !existing.getBookingId().equals(ignoredBookingId))
                .anyMatch(existing -> {
                    LocalDateTime existingStart = getOccupiedStart(existing);
                    LocalDateTime existingEnd = getOccupiedEnd(existing);
                    return bookingStart.isBefore(existingEnd) && bookingEnd.isAfter(existingStart);
                });
    }


    private AvailableGroomerResponse toAvailableGroomerResponse(StaffSchedule schedule) {
        User staff = schedule.getStaff();
        return AvailableGroomerResponse.builder()
                .staffId(staff.getUserId())
                .fullName(staff.getFullName())
                .specialization(staff.getSpecialization())
                .introduction(staff.getIntroduction())
                .yearsOfExperience(staff.getYearsOfExperience())
                .avatar(resolveUserAvatar(staff))
                .shiftStart(schedule.getWorkSchedule().getStartTime())
                .shiftEnd(schedule.getWorkSchedule().getEndTime())
                .build();
    }

    private String resolveUserAvatar(User user) {
        if (user == null || user.getMediaFiles() == null) {
            return null;
        }
        return user.getMediaFiles().stream()
                .map(MediaFile::getFileUrl)
                .filter(fileUrl -> fileUrl != null && !fileUrl.isBlank())
                .findFirst()
                .orElse(null);
    }

    private int calculateBookingDurationForRequest(List<BookingDetailCreationRequest> detailRequests, User user) {
        return detailRequests.stream()
                .mapToInt(detailRequest -> {
                    Catalog catalog = catalogRepository.findById(detailRequest.getCatalogId())
                            .orElseThrow(() -> new AppException(ErrorCode.CATALOG_NOT_FOUND));
                    PetProfile pet = petRepository.findById(detailRequest.getPetId())
                            .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));
                    if (user.getRole() == Role.CUSTOMER && !pet.getUser().getUserId().equals(user.getUserId())) {
                        throw new AppException(ErrorCode.PET_OWNER_INVALID);
                    }
                    return calculatePricingSnapshot(catalog, pet).totalDurationMinute();
                })
                .sum();
    }

    private PricingSnapshot calculatePricingSnapshot(Catalog catalog, PetProfile pet) {
        WeightRange weightRange = resolvePetWeightRange(pet);
        int baseDurationMinute = catalog.getDurationMinute() == null ? 0 : catalog.getDurationMinute();
        int additionalDurationMinute = parseAdditionalDuration(catalog.getAdditionalDurationConfig(), weightRange);
        BigDecimal basePrice = catalog.getPrice() == null ? BigDecimal.ZERO : catalog.getPrice();
        BigDecimal additionalPricePerMinute = catalog.getAdditionalPricePerMinute() == null
                ? BigDecimal.ZERO
                : catalog.getAdditionalPricePerMinute();
        if (additionalPricePerMinute.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_ADDITIONAL_PRICE_PER_MINUTE);
        }
        BigDecimal additionalPrice = additionalPricePerMinute.multiply(BigDecimal.valueOf(additionalDurationMinute));
        return new PricingSnapshot(
                weightRange,
                baseDurationMinute,
                additionalDurationMinute,
                baseDurationMinute + additionalDurationMinute,
                basePrice,
                additionalPrice,
                basePrice.add(additionalPrice)
        );
    }

    private WeightRange resolvePetWeightRange(PetProfile pet) {
        if (pet.getWeight() == null) {
            throw new AppException(ErrorCode.PET_WEIGHT_REQUIRED);
        }
        validatePetWeight(pet.getWeight());
        return WeightRange.fromWeight(pet.getWeight());
    }

    private void validatePetWeight(Double weight) {
        if (weight == null || weight <= 0 || weight > 100) {
            throw new AppException(ErrorCode.INVALID_PET_WEIGHT);
        }
    }

    private int parseAdditionalDuration(String config, WeightRange weightRange) {
        if (config == null || config.isBlank()) {
            return 0;
        }
        for (String part : config.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) {
                throw new AppException(ErrorCode.INVALID_ADDITIONAL_DURATION_CONFIG);
            }
            if (kv[0].trim().equalsIgnoreCase(weightRange.name())) {
                try {
                    int minutes = Integer.parseInt(kv[1].trim());
                    if (minutes < 0) {
                        throw new AppException(ErrorCode.INVALID_ADDITIONAL_DURATION_CONFIG);
                    }
                    return minutes;
                } catch (NumberFormatException ex) {
                    throw new AppException(ErrorCode.INVALID_ADDITIONAL_DURATION_CONFIG);
                }
            }
        }
        return 0;
    }

    private void validateAssignedStaffCheckedIn(Booking booking) {
        StaffSchedule schedule = booking.getStaffSchedule();
        if (schedule == null
                || schedule.getScheduleStatus() != ScheduleStatus.WORKING
                || schedule.getCheckInAt() == null) {
            throw new AppException(ErrorCode.STAFF_NOT_CHECKED_IN);
        }
    }

    private void validateHomeDepartureTime(Booking booking) {
        if (!isHomeBooking(booking)) {
            return;
        }
        int travelMinute = booking.getEstimatedTravelMinute() == null || booking.getEstimatedTravelMinute() <= 0
                ? DEFAULT_HOME_TRAVEL_MINUTE
                : booking.getEstimatedTravelMinute();
        LocalDateTime earliestDeparture = booking.getScheduledAt().minusMinutes(travelMinute + 15L);
        if (LocalDateTime.now(VIETNAM_ZONE).isBefore(earliestDeparture)) {
            throw new AppException(ErrorCode.INVALID_HOME_BOOKING_STATUS);
        }
    }

    private void validateNoOtherInProgressBooking(Booking booking) {
        boolean hasOtherInProgress = bookingRepository.findByStaffScheduleAndBookingStatusIn(
                        booking.getStaffSchedule(),
                        List.of(BookingStatus.IN_PROGRESS)
                )
                .stream()
                .anyMatch(existing -> !existing.getBookingId().equals(booking.getBookingId()));
        if (hasOtherInProgress) {
            throw new AppException(ErrorCode.STAFF_BOOKING_OVERLAP);
        }
    }

    private record PricingSnapshot(
            WeightRange weightRange,
            int baseDurationMinute,
            int additionalDurationMinute,
            int totalDurationMinute,
            BigDecimal basePrice,
            BigDecimal additionalPrice,
            BigDecimal totalPrice
    ) {}

    private record HomeFeeSnapshot(
            Double distanceKm,
            BigDecimal travelFee,
            Integer estimatedTravelMinute
    ) {}

}
