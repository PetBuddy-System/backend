package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperSuggestionResponse {
    String staffId;
    String staffName;
    int currentLoad;
    int maxCapacity;
    Double distanceToClusterKm;
}
