package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnItemRequest {
    @NotNull(message = "ORDER_DETAIL_NOT_FOUND")
    Long orderDetailId;

    @NotNull(message = "RETURN_QUANTITY_INVALID")
    @Min(value = 1, message = "RETURN_QUANTITY_INVALID")
    Integer quantity;
}
