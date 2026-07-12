package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RestockItemResponse {
    Long orderDetailId;
    String productName;
    String productImage;
    Integer requestedQuantity;
    List<RestockBatchResponse> batches;
}