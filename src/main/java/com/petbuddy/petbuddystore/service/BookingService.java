package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import com.petbuddy.petbuddystore.common.enums.BookingMediaType;
import com.petbuddy.petbuddystore.dto.request.AvailableGroomerRequest;
import com.petbuddy.petbuddystore.dto.request.BookingCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BookingUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.AvailableGroomerResponse;
import com.petbuddy.petbuddystore.dto.response.BookingResponse;
import com.petbuddy.petbuddystore.dto.response.MediaFileResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    BookingResponse createBooking(BookingCreationRequest request);
    List<BookingResponse> getMyBookings();
    PaymentResponse retryPayment(Integer bookingId);
    List<BookingResponse> getBookings(BookingStatus status, LocalDate fromDate, LocalDate toDate);
    BookingResponse getBooking(Integer bookingId);
    List<AvailableGroomerResponse> getAvailableGroomers(AvailableGroomerRequest request);
    BookingResponse updateStatus(Integer bookingId, BookingUpdateRequest request);
    BookingResponse assignGroomer(Integer bookingId, String groomerId);
    MediaFileResponse uploadBookingMedia(Integer bookingDetailId, BookingMediaType bookingMediaType, MultipartFile file);
    void autoReassignOverdueBookings();
}
