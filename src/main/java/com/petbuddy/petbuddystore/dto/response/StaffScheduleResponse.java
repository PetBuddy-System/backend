package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.common.enums.ShiftType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffScheduleResponse {
    String staffScheduleId;
    String staffId;
    String staffName;
    String workScheduleId;
    LocalDate workDate;
    LocalTime startTime;
    LocalTime endTime;
    ShiftType shiftType;
    ScheduleStatus scheduleStatus;
    LocalDateTime assignedAt;
    LocalDateTime checkInAt;
    LocalDateTime checkOutAt;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
