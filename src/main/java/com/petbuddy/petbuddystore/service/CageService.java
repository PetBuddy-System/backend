package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.CageSize;
import com.petbuddy.petbuddystore.common.enums.CageStatus;
import com.petbuddy.petbuddystore.dto.request.CageCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CageUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CageResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CageService {
    List<CageResponse> createCage(CageCreationRequest request);
    Page<CageResponse> getCages(CageSize cageSize, CageStatus cageStatus, int page, int size);
    CageResponse getCageById(Long cageId);
    CageResponse updateCage(CageUpdateRequest request, Long cageId);
}