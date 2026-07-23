package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationItem;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ShiftRegistrationResponse;
import com.petbuddy.petbuddystore.model.StaffShiftRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StaffShiftRegistrationMapper {
    StaffShiftRegistration toStaffShiftRegistration(ShiftRegistrationItem request);

    @Mapping(source = "staff.userId", target = "staffId")
    @Mapping(source = "staff.fullName", target = "staffName")
    @Mapping(source = "staff.email", target = "staffEmail")
    ShiftRegistrationResponse toShiftRegistrationResponse(StaffShiftRegistration staffShiftRegistration);

}
