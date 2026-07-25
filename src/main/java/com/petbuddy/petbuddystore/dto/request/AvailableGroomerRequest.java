package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailableGroomerRequest {
    @NotNull
    LocalDateTime scheduledAt;

    String bookingType;

    Double latitude;

    Double longitude;

    Integer estimatedTravelMinute;

    @Valid
    @NotEmpty
    List<BookingDetailCreationRequest> bookingDetails;
}
