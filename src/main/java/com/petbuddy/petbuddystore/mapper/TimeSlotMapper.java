package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.TimeSlotCreationRequest;
import com.petbuddy.petbuddystore.dto.request.TimeSlotUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.TimeSlotResponse;
import com.petbuddy.petbuddystore.model.CatalogTimeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper (componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)

public interface TimeSlotMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    CatalogTimeSlot toCatalogTimeSlot(TimeSlotCreationRequest request);

    @Mapping(target = "timeSlotId", source = "id")
    @Mapping(target = "catalogId", source = "catalog.catalogId")
    @Mapping(target = "catalogName", source = "catalog.catalogName")
    @Mapping(target = "durationMinute", source = "catalog.durationMinute")
    TimeSlotResponse toCatalogTimeSlotResponse(CatalogTimeSlot catalogTimeSlot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "catalog", ignore = true)
    void updateCatalogTimeSlot(
            @MappingTarget CatalogTimeSlot catalogTimeSlot,
            TimeSlotUpdateRequest request
    );
}
