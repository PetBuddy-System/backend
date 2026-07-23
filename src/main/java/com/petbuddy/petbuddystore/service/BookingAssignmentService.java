package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.model.Booking;
import com.petbuddy.petbuddystore.model.StaffSchedule;

import java.time.LocalDateTime;

public interface BookingAssignmentService {
    void validateSelectedGroomer(Booking booking, String groomerId);
    void assignAfterPaymentSucceeded(Booking booking);
    void assignManual(Booking booking, String groomerId);
    StaffSchedule findBestAvailableGroomer(LocalDateTime bookingStart, LocalDateTime bookingEnd, String excludedStaffId);
}
