package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    String resetToken;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String confirmNewPassword;

}
