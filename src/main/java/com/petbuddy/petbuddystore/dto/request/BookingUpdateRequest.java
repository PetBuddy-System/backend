package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingUpdateRequest {
    @NotNull
    BookingStatus status;

    String cancelReason;
}
