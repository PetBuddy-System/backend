package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import com.petbuddy.petbuddystore.common.enums.StaffAssignmentMode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {
    Integer bookingId;
    String bookingCode;
    String bookingType;
    String customerName;
    String customerPhone;
    String address;
    Double latitude;
    Double longitude;
    String addressNote;
    Double distanceKm;
    BigDecimal travelFee;
    Integer estimatedTravelMinute;
    Boolean homeServiceRequirementsAccepted;
    LocalDateTime scheduledAt;
    LocalDateTime estimatedEndAt;
    BookingStatus bookingStatus;
    BigDecimal totalAmount;
    BigDecimal depositAmount;
    BigDecimal remainingAmount;
    String note;
    String cancelReason;
    LocalDateTime paymentDeadlineAt;
    StaffAssignmentMode assignmentMode;
    String requestedStaffId;
    String requestedStaffName;
    String staffScheduleId;
    String staffId;
    String staffName;
    String assignedStaffId;
    String assignedStaffName;
    LocalDateTime departedAt;
    LocalDateTime actualStartedAt;
    LocalDateTime actualCompletedAt;
    String stripeClientSecret;
    List<BookingDetailResponse> bookingDetails;
}
