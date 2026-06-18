package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.dto.response.StaffScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface StaffScheduleService {
    List<StaffScheduleResponse> getMySchedules(LocalDate fromDate, LocalDate toDate, ScheduleStatus status);
    StaffScheduleResponse checkIn(String staffScheduleId);
    StaffScheduleResponse checkOut(String staffScheduleId);
}
