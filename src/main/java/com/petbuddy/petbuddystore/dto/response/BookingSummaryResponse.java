package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Dùng cho danh sách (ít field hơn BookingResponse)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingSummaryResponse {
    Integer bookingId;
    String bookingCode;
    BookingStatus bookingStatus;
    LocalDateTime scheduledAt;
    String customerName;
    String customerPhone;
    BigDecimal totalAmount;
    int petCount;
    LocalDateTime createdAt;
    WorkScheduleSlotResponse workSchedule;
}
