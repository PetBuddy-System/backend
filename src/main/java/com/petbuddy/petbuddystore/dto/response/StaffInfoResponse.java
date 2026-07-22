package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Thông tin staff rút gọn để nhúng vào BookingResponse
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffInfoResponse {
    String staffId;
    String fullName;
    String email;
    String specialization;
    String introduction;
    Integer yearsOfExperience;
    String avatar;
}
