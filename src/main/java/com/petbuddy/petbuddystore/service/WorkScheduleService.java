package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.dto.request.StaffReassignRequest;
import com.petbuddy.petbuddystore.dto.request.StaffsAssignRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleCreationRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.StaffScheduleResponse;
import com.petbuddy.petbuddystore.dto.response.WorkScheduleResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface WorkScheduleService {
    WorkScheduleResponse createWorkSchedule(WorkScheduleCreationRequest request);
    WorkScheduleResponse getWorkScheduleById(String workScheduleId);
    Page<WorkScheduleResponse> getWorkSchedules(LocalDate fromDate, LocalDate toDate, ShiftType shiftType,
            int page, int size);
    WorkScheduleResponse updateWorkSchedule(String workScheduleId, WorkScheduleUpdateRequest request);
    WorkScheduleResponse assignStaffsToWorkSchedule(String workScheduleId, StaffsAssignRequest request);
    void removeStaffFromWorkSchedule(String staffScheduleId);
    StaffScheduleResponse reassignStaffToWorkSchedule(String staffScheduleId, StaffReassignRequest request);

}
