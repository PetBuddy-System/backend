package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.StaffTask;
import com.petbuddy.petbuddystore.common.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String userId;
    String email;
    String fullName;
    String gender;
    LocalDate dateOfBirth;
    String role;
    StaffTask staffTask;
    UserStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
