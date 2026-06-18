package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffAssignedResponse {
    String staffScheduleId;
    String staffId;
    String staffEmail;
    String staffName;
    ScheduleStatus scheduleStatus;
    LocalDateTime checkInAt;
    LocalDateTime checkOutAt;
    LocalDateTime assignedAt;
    String note;
}
