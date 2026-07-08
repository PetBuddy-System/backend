package com.petbuddy.petbuddystore.mapper;


import com.petbuddy.petbuddystore.dto.request.StoreLocationRequest;
import com.petbuddy.petbuddystore.dto.response.StoreLocationResponse;
import com.petbuddy.petbuddystore.model.StoreLocation;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreLocationMapper {
    StoreLocation toEntity(StoreLocationRequest request);
    StoreLocationResponse toResponse(StoreLocation entity);
    List<StoreLocationResponse> toResponseList(List<StoreLocation> entities);
}
