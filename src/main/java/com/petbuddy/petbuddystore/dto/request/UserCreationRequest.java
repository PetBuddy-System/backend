package com.petbuddy.petbuddystore.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.StaffTask;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "INVALID_EMAIL")
    String email;

    @Size(min = 8, message = "PASSWORD_INVALID")
    String password;

    @NotBlank(message = "FULL_NAME_REQUIRED")
    String fullName;

    @NotBlank(message = "GENDER_REQUIRED")
    String gender;

    @NotNull(message = "DATE_OF_BIRTH_REQUIRED")
    LocalDate dateOfBirth;

    Role role;
    StaffTask staffTask;
}
