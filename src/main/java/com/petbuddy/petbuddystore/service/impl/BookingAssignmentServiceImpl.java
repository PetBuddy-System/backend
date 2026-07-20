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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingAssignmentServiceImpl implements BookingAssignmentService {
    static final List<BookingStatus> STAFF_OCCUPYING_STATUSES = List.of(
            BookingStatus.ACCEPTED,
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
        LocalDateTime bookingStart = booking.getScheduledAt();
        LocalDateTime bookingEnd = bookingStart.plusMinutes(getBookingDuration(booking));
        findAssignableScheduleForGroomer(groomer, bookingStart, bookingEnd, booking.getBookingId());
    }

    @Override
    public void assignAfterPaymentSucceeded(Booking booking) {
        LocalDateTime bookingStart = booking.getScheduledAt();
        LocalDateTime bookingEnd = bookingStart.plusMinutes(getBookingDuration(booking));

        try {
            StaffSchedule staffSchedule;
            if (booking.getAssignmentMode() == StaffAssignmentMode.SELECTED) {
                if (booking.getRequestedStaffId() == null || booking.getRequestedStaffId().isBlank()) {
                    throw new AppException(ErrorCode.REQUESTED_GROOMER_REQUIRED);
                }
                User groomer = userRepository.findById(booking.getRequestedStaffId())
                        .orElseThrow(() -> new AppException(ErrorCode.REQUESTED_GROOMER_NOT_AVAILABLE));
                staffSchedule = findAssignableScheduleForGroomer(groomer, bookingStart, bookingEnd, booking.getBookingId());
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
        LocalDateTime bookingStart = booking.getScheduledAt();
        LocalDateTime bookingEnd = bookingStart.plusMinutes(getBookingDuration(booking));
        StaffSchedule staffSchedule = findAssignableScheduleForGroomer(groomer, bookingStart, bookingEnd, booking.getBookingId());

        applyAssignment(booking, staffSchedule);
        booking.setBookingStatus(BookingStatus.ACCEPTED);
        booking.setAssignmentMode(StaffAssignmentMode.SELECTED);
        booking.setRequestedStaffId(groomer.getUserId());
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
            LocalDateTime bookingStart,
            LocalDateTime bookingEnd,
            Integer ignoredBookingId
    ) {
        if (groomer.getRole() != Role.STAFF
                || groomer.getStaffTask() != StaffTask.GROOMER
                || groomer.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.REQUESTED_GROOMER_NOT_AVAILABLE);
        }

        StaffSchedule schedule = staffScheduleRepository
                .findScheduleForDate(groomer.getUserId(), bookingStart.toLocalDate(), ASSIGNABLE_SCHEDULE_STATUSES)
                .orElseThrow(() -> new AppException(ErrorCode.REQUESTED_GROOMER_NOT_AVAILABLE));

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
                    LocalDateTime existingStart = existing.getScheduledAt();
                    LocalDateTime existingEnd = existingStart.plusMinutes(getBookingDuration(existing));
                    return bookingStart.isBefore(existingEnd) && bookingEnd.isAfter(existingStart);
                });
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
        LocalDateTime now = LocalDateTime.now();
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
}
