package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.model.Booking;
import com.petbuddy.petbuddystore.model.BookingDetail;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.BookingRepository;
import com.petbuddy.petbuddystore.repository.StaffScheduleRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.BookingAssignmentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingAssignmentServiceImpl implements BookingAssignmentService {
    static final int DEFAULT_HOME_TRAVEL_MINUTE = 30;
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
    StaffScheduleRepository staffScheduleRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public void validateSelectedGroomer(Booking booking, String groomerId) {
        if (groomerId == null || groomerId.isBlank()) {
            throw new AppException(ErrorCode.REQUESTED_GROOMER_REQUIRED);
        }
        User groomer = userRepository.findById(groomerId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUESTED_GROOMER_NOT_AVAILABLE));
        validateGroomerProfile(groomer);
    }

    @Override
    public void assignAfterPaymentSucceeded(Booking booking) {
        LocalDateTime bookingStart = getOccupiedStart(booking);
        LocalDateTime bookingEnd = getOccupiedEnd(booking);

        try {
            StaffSchedule staffSchedule;
            if (booking.getAssignmentMode() == StaffAssignmentMode.SELECTED) {
                if (booking.getRequestedStaffId() == null || booking.getRequestedStaffId().isBlank()) {
                    throw new AppException(ErrorCode.REQUESTED_GROOMER_REQUIRED);
                }
                User groomer = userRepository.findById(booking.getRequestedStaffId())
                        .orElseThrow(() -> new AppException(ErrorCode.REQUESTED_GROOMER_NOT_AVAILABLE));
                staffSchedule = findAssignableScheduleForGroomer(groomer, booking.getScheduledAt().toLocalDate(), bookingStart, bookingEnd, booking.getBookingId(), isHomeBooking(booking));
            } else {
                staffSchedule = findBestAvailableGroomer(bookingStart, bookingEnd, null);
                if (staffSchedule == null) {
                    throw new AppException(ErrorCode.NO_AVAILABLE_GROOMER);
                }
            }

            applyAssignment(booking, staffSchedule);
            booking.setBookingStatus(BookingStatus.ACCEPTED);
        } catch (AppException ex) {
            booking.setStaffSchedule(null);
            booking.setBookingStatus(BookingStatus.WAITING_STAFF);
            log.warn("Booking {} moved to WAITING_STAFF after payment: {}", booking.getBookingId(), ex.getErrorCode());
        }
    }

    @Override
    public void assignManual(Booking booking, String groomerId) {
        if (EnumSet.of(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, BookingStatus.CANCELLED, BookingStatus.FAILED)
                .contains(booking.getBookingStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        User groomer = userRepository.findById(groomerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        LocalDateTime bookingStart = getOccupiedStart(booking);
        LocalDateTime bookingEnd = getOccupiedEnd(booking);
        StaffSchedule staffSchedule = findAssignableScheduleForGroomer(groomer, booking.getScheduledAt().toLocalDate(), bookingStart, bookingEnd, booking.getBookingId(), isHomeBooking(booking));

        applyAssignment(booking, staffSchedule);
        booking.setBookingStatus(BookingStatus.ACCEPTED);
    }

    @Override
    public StaffSchedule findBestAvailableGroomer(LocalDateTime bookingStart, LocalDateTime bookingEnd, String excludedStaffId) {
        return staffScheduleRepository.findGroomerSchedulesForAssignment(
                        bookingStart.toLocalDate(),
                        ASSIGNABLE_SCHEDULE_STATUSES,
                        StaffTask.GROOMER,
                        UserStatus.ACTIVE
                )
                .stream()
                .filter(schedule -> excludedStaffId == null || !schedule.getStaff().getUserId().equals(excludedStaffId))
                .filter(schedule -> isScheduleAssignable(schedule, bookingStart, bookingEnd, null))
                .min(Comparator
                        .comparingInt(this::countStaffBookingsForRoundRobin)
                        .thenComparing(schedule -> schedule.getAssignedAt() == null ? LocalDateTime.MIN : schedule.getAssignedAt())
                        .thenComparing(schedule -> schedule.getStaff().getUserId()))
                .orElse(null);
    }

    private StaffSchedule findAssignableScheduleForGroomer(
            User groomer,
            LocalDate scheduleDate,
            LocalDateTime bookingStart,
            LocalDateTime bookingEnd,
            Integer ignoredBookingId,
            boolean travelBufferIncluded
    ) {
        validateGroomerProfile(groomer);

        StaffSchedule schedule = staffScheduleRepository
                .findScheduleForDate(groomer.getUserId(), scheduleDate, ASSIGNABLE_SCHEDULE_STATUSES)
                .orElseThrow(() -> new AppException(ErrorCode.REQUESTED_GROOMER_NOT_AVAILABLE));

        if (!isWithinShift(schedule, bookingStart, bookingEnd)) {
            throw new AppException(travelBufferIncluded
                    ? ErrorCode.STAFF_TRAVEL_TIME_NOT_ENOUGH
                    : ErrorCode.BOOKING_OUTSIDE_STAFF_SHIFT);
        }
        if (!isScheduleAssignable(schedule, bookingStart, bookingEnd, ignoredBookingId)) {
            throw new AppException(ErrorCode.REQUESTED_GROOMER_NOT_AVAILABLE);
        }
        return schedule;
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
        if (!isWithinShift(schedule, bookingStart, bookingEnd)) {
            return false;
        }
        return !hasStaffOverlap(schedule, bookingStart, bookingEnd, ignoredBookingId);
    }

    private boolean isWithinShift(StaffSchedule schedule, LocalDateTime bookingStart, LocalDateTime bookingEnd) {
        if (schedule == null || schedule.getWorkSchedule() == null) {
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
        return !bookingStart.isBefore(shiftStart) && !bookingEnd.isAfter(shiftEnd);
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
                    LocalDateTime existingStart = existing.getScheduledAt();
                    LocalDateTime existingEnd = getOccupiedEnd(existing);
                    existingStart = getOccupiedStart(existing);
                    return bookingStart.isBefore(existingEnd) && bookingEnd.isAfter(existingStart);
                });
    }

    private void validateGroomerProfile(User groomer) {
        if (groomer.getRole() != Role.STAFF
                || groomer.getStaffTask() != StaffTask.GROOMER
                || groomer.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.REQUESTED_GROOMER_NOT_AVAILABLE);
        }
    }

    private int countStaffBookingsForRoundRobin(StaffSchedule schedule) {
        LocalDateTime startOfDay = schedule.getWorkSchedule().getWorkDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return bookingRepository.findByStaffScheduleAndBookingStatusInAndScheduledAtBetween(
                        schedule,
                        STAFF_OCCUPYING_STATUSES,
                        startOfDay,
                        endOfDay
                )
                .size();
    }

    private void applyAssignment(Booking booking, StaffSchedule staffSchedule) {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        booking.setStaffSchedule(staffSchedule);
        staffSchedule.setAssignedAt(now);
        staffScheduleRepository.save(staffSchedule);
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

    private boolean isHomeBooking(Booking booking) {
        return LocationType.AT_HOME.name().equals(booking.getBookingType());
    }
}
