package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.WorkScheduleCreationRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.StaffAssignedResponse;
import com.petbuddy.petbuddystore.dto.response.WorkScheduleResponse;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import com.petbuddy.petbuddystore.model.WorkSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WorkScheduleMapper {
    WorkSchedule toWorkSchedule(WorkScheduleCreationRequest request);

    @Mapping(source = "staffSchedules", target = "assignedStaffs")
    WorkScheduleResponse toWorkScheduleResponse(WorkSchedule workSchedule);

    @Mapping(source = "staff.userId", target = "staffId")
    @Mapping(source = "staff.fullName", target = "staffName")
    @Mapping(source = "staff.email", target = "staffEmail")
    @Mapping(source = "scheduleStatus", target = "scheduleStatus")
    StaffAssignedResponse toStaffAssignedResponse(StaffSchedule staffSchedule);

    void updateWorkSchedule(@MappingTarget WorkSchedule workSchedule, WorkScheduleUpdateRequest request);
}
