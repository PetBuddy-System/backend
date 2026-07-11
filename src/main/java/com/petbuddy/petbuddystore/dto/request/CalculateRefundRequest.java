package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CalculateRefundRequest {
    @NotNull(message = "ORDER_NOT_FOUND")
    Long orderId;

    @NotNull(message = "INVALID_KEY")
    String reason;

    @NotEmpty(message = "INVALID_KEY")
    @Valid
    List<ReturnItemRequest> items;
}
