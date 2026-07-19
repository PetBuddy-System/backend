package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryFailedRequest {
    @NotBlank(message = "Reason for delivery failure must not be blank")
    private String reason;
}
