package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.ShiftType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

// Slot WorkSchedule kèm thông tin lồng còn trống
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkScheduleSlotResponse {
    String workScheduleId;
    LocalDate workDate;
    LocalTime startTime;
    LocalTime endTime;
    ShiftType shiftType;

    // Chỉ có khi gọi API slot (API 1.1), null trong các context khác
    Integer totalAvailableCages;
    Map<String, Integer> availableBySize;
}
