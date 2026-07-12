package com.petbuddy.petbuddystore.scheduler;

import com.petbuddy.petbuddystore.common.enums.AttendanceStatus;
import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import com.petbuddy.petbuddystore.model.WorkSchedule;
import com.petbuddy.petbuddystore.repository.StaffScheduleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttendanceScheduler {
    StaffScheduleRepository staffScheduleRepository;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void markAbsentStaffSchedules() {
        LocalDateTime now = LocalDateTime.now();
        List<StaffSchedule> schedules = staffScheduleRepository.findByScheduleStatus(ScheduleStatus.SCHEDULED);

        for (StaffSchedule schedule : schedules) {
            WorkSchedule workSchedule = schedule.getWorkSchedule();
            LocalDateTime absentTime = LocalDateTime.of(workSchedule.getWorkDate(), workSchedule.getStartTime()).plusMinutes(30);

            if (now.isAfter(absentTime)) {
                schedule.setAttendanceStatus(AttendanceStatus.ABSENT);
                schedule.setScheduleStatus(ScheduleStatus.ABSENT);
            }
        }
    }
}
