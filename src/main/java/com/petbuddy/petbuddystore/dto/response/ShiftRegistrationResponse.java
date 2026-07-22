package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ShiftType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftRegistrationResponse {
    String registrationId;
    String staffId;
    String staffName;
    LocalDate workDate;
    ShiftType preferredShift;
    LocalTime preferredStartTime;
    LocalTime preferredEndTime;
    String reason;
    LocalDateTime createdAt;
}
