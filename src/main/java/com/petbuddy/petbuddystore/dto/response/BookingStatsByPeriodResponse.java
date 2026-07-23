package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingStatsByPeriodResponse {
    String period;        // Ví dụ: "2026-07-15" hoặc "2026-07" hoặc "2026-W28"
    BigDecimal revenue;
    long bookingCount;
}
