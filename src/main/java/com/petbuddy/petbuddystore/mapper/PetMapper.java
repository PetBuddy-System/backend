package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.PetProfileCreationRequest;
import com.petbuddy.petbuddystore.dto.request.PetProfileUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.PetProfileResponse;
import com.petbuddy.petbuddystore.model.PetProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PetMapper {
    PetProfile toPetProfile(PetProfileCreationRequest request);

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "mediaFiles", target = "mediaFiles")
    PetProfileResponse toPetProfileResponse(PetProfile petProfile);

    void updatePet(PetProfileUpdateRequest request, @MappingTarget PetProfile petProfile);
}
