package com.petbuddy.petbuddystore.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RestockItemRequest {
    Long orderDetailId;
    List<RestockBatchRequest> batches;
}