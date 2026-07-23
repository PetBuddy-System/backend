package com.petbuddy.petbuddystore.dto.request;


import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.StaffTask;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    String fullName;
    String gender;
    LocalDate dateOfBirth;
    Role role;
    StaffTask staffTask;
    String specialization;
    String introduction;
    Integer yearsOfExperience;
}
