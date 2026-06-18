package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.common.enums.ShiftType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkScheduleCreationRequest {
    LocalDate workDate;
    LocalTime startTime;
    LocalTime endTime;
    String note;
    ShiftType shiftType;
    List<String> staffIds;
}
