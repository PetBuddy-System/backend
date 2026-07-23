package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingPreviewResponse {
    BigDecimal serviceAmount;
    BigDecimal surchargeAmount;
    BigDecimal travelFee;
    Double distanceKm;
    Integer estimatedTravelMinute;
    Integer estimatedServiceMinute;
    BigDecimal totalAmount;
    BigDecimal depositAmount;
}
