package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateReturnStatusRequest {
    @NotNull(message = "INVALID_KEY")
    String status;

    String staffNote;
}
