package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.RegistrationPeriodCreationRequest;
import com.petbuddy.petbuddystore.dto.response.RegistrationPeriodResponse;
import com.petbuddy.petbuddystore.model.ShiftRegistrationPeriod;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ShiftRegistrationPeriodMapper {
    ShiftRegistrationPeriod toShiftRegistrationPeriod(RegistrationPeriodCreationRequest request);

    RegistrationPeriodResponse toRegistrationPeriodResponse(ShiftRegistrationPeriod shiftRegistrationPeriod);
}
