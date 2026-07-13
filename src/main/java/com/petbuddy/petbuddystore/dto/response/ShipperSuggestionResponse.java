package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.StaffTask;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperSuggestionResponse {
    String staffId;
    String staffEmail;
    String staffName;
    StaffTask staffTask;
    int currentLoad;
    int maxCapacity;
    Double distanceToClusterKm;
}
