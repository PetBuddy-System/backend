package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingStatsSummaryResponse {
    BigDecimal totalRevenue;
    long totalBookings;
    long completedBookings;
    long cancelledBookings;
    long pendingBookings;
    BigDecimal averageOrderValue;
}
