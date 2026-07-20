package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvailableGroomerResponse {
    String staffId;
    String fullName;
    String specialization;
    String introduction;
    Integer yearsOfExperience;
    String avatar;
    LocalTime shiftStart;
    LocalTime shiftEnd;
}
