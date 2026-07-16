package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.DayOfWeek;
import java.time.LocalTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TimeSlotResponse {
    Integer timeSlotId;

    Integer catalogId;

    String catalogName;

    Integer durationMinute;

    DayOfWeek dayOfWeek;

    LocalTime startTime;

    Boolean isActive;

    Integer maxPets;
}
