package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.PetStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.PetProfileCreationRequest;
import com.petbuddy.petbuddystore.dto.request.PetProfileUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.PetProfileResponse;
import com.petbuddy.petbuddystore.mapper.PetMapper;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.model.PetProfile;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.PetRepository;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.PetService;
import com.petbuddy.petbuddystore.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PetServiceImpl implements PetService {
    PetRepository petRepository;
    PetMapper petMapper;
    FileService fileService;
    UserService userService;

    @Override
    public PetProfileResponse createPet(PetProfileCreationRequest request, List<MultipartFile> images) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserEntityById(userId);

        PetProfile petProfile = petMapper.toPetProfile(request);
        petProfile.setUser(user);
        petProfile.setPetStatus(PetStatus.ACTIVE);

        List<MediaFile> mediaFiles = images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .map(file -> {
                    MediaFile mediaFile = fileService.uploadPetImage(file);
                    mediaFile.setPetProfile(petProfile);
                    return mediaFile;
                })
                .collect(Collectors.toList());

        petProfile.setMediaFiles(mediaFiles);
        return petMapper.toPetProfileResponse(petRepository.save(petProfile));
    }

    @Override
    public PetProfileResponse getPetById(String petId) {
        PetProfile petProfile = petRepository.findById(petId)
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));
        return petMapper.toPetProfileResponse(petProfile);
    }

    @Override
    public PetProfileResponse updatePet(String petId, PetProfileUpdateRequest request, List<MultipartFile> images) {
        PetProfile petProfile = petRepository.findById(petId)
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));
        petMapper.updatePet(request, petProfile);

        petProfile.getMediaFiles().clear();
        List<MediaFile> mediaFiles = images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .map(file -> {
                    MediaFile mediaFile = fileService.uploadPetImage(file);
                    mediaFile.setPetProfile(petProfile);
                    return mediaFile;
                })
                .toList();

        if (!mediaFiles.isEmpty()) {
            petProfile.getMediaFiles().addAll(mediaFiles);
        }
        return petMapper.toPetProfileResponse(petRepository.save(petProfile));
    }
}
