package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RestockEligibilityResponse {
    String staffId;
    String staffName;
    int currentPendingOrders;
    double remainingShiftMinutes;
    double distanceToStoreKm;
    boolean eligibleForRestock;
    int estimatedExtraOrders;
}
