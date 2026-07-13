package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
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
    LocalDateTime scheduledAt;
    BookingStatus bookingStatus;
    BigDecimal totalAmount;
    BigDecimal depositAmount;
    BigDecimal remainingAmount;
    String note;
    String cancelReason;
    LocalDateTime paymentDeadlineAt;
    String staffScheduleId;
    String staffId;
    String staffName;
    String stripeClientSecret;
    List<BookingDetailResponse> bookingDetails;
}
