package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.PetProfileCreationRequest;
import com.petbuddy.petbuddystore.dto.request.PetProfileUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.PetProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PetService {
    PetProfileResponse createPet(PetProfileCreationRequest request, List<MultipartFile> images);
    PetProfileResponse getPetById(String petId);
    PetProfileResponse updatePet(String petId, PetProfileUpdateRequest request, List<MultipartFile> images);
}
