package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TimeSlotCreationRequest {
    @NotNull
    Integer catalogId;

    @NotNull
    DayOfWeek dayOfWeek;

    @NotNull
    LocalTime startTime;

    Boolean isActive = true;

    @Positive
    Integer maxPets = 5;
}
