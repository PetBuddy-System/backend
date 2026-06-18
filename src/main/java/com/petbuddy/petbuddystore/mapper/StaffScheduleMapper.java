package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.StaffScheduleResponse;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StaffScheduleMapper {
    @Mapping(target = "staffId", source = "staff.userId")
    @Mapping(target = "staffName", source = "staff.fullName")
    @Mapping(target = "workScheduleId", source = "workSchedule.workScheduleId")
    @Mapping(target = "workDate", source = "workSchedule.workDate")
    @Mapping(target = "startTime", source = "workSchedule.startTime")
    @Mapping(target = "endTime", source = "workSchedule.endTime")
    @Mapping(target = "shiftType", source = "workSchedule.shiftType")
    StaffScheduleResponse toStaffScheduleResponse(StaffSchedule staffSchedule);
}
