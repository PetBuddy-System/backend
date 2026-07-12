package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RestockBatchResponse {
    UUID batchId;
    String batchCode;
    Integer deductedQuantity;
    Integer availableToRestock;
    Integer restockQuantity;
}