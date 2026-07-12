package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetailCreationRequest {
    @NotNull
    String petId;

    @NotNull
    Integer catalogId;

    @NotNull
    Integer timeSlotId;

    String note;
}
