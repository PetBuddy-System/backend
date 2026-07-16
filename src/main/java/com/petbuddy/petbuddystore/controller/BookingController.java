package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import com.petbuddy.petbuddystore.common.enums.BookingMediaType;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.BookingCreationRequest;
import com.petbuddy.petbuddystore.dto.request.BookingUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.BookingResponse;
import com.petbuddy.petbuddystore.dto.response.MediaFileResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Booking API", description = "Quản lý đặt lịch dịch vụ")
public class BookingController {
    BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingCreationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", bookingService.createBooking(request)));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings() {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getMyBookings()));
    }

    @PostMapping("/{bookingId}/retry-payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(@PathVariable Integer bookingId) {
        return ResponseEntity.ok(ApiResponse.success("Payment recreated successfully", bookingService.retryPayment(bookingId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or (hasRole('STAFF') and (hasAuthority('TASK_GROOMER') or hasAuthority('TASK_COORDINATOR')))")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookings(status, fromDate, toDate)));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER') or (hasRole('STAFF') and (hasAuthority('TASK_GROOMER') or hasAuthority('TASK_COORDINATOR')))")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Integer bookingId) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBooking(bookingId)));
    }

    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_GROOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> updateStatus(
            @PathVariable Integer bookingId,
            @Valid @RequestBody BookingUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Booking status updated successfully", bookingService.updateStatus(bookingId, request)));
    }

    @PatchMapping("/{bookingId}/assign-groomer")
    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_COORDINATOR')")
    public ResponseEntity<ApiResponse<BookingResponse>> assignGroomer(
            @PathVariable Integer bookingId,
            @RequestParam String groomerId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Booking assigned to groomer successfully",
                bookingService.assignGroomer(bookingId, groomerId)));
    }

    @PostMapping(value = "/details/{bookingDetailId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_GROOMER')")
    public ResponseEntity<ApiResponse<MediaFileResponse>> uploadBookingMedia(
            @PathVariable Integer bookingDetailId,
            @RequestParam BookingMediaType type,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking media uploaded successfully",
                        bookingService.uploadBookingMedia(bookingDetailId, type, file)));
    }
}
