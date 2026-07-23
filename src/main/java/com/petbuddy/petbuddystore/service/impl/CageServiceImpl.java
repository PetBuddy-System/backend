package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.CageSize;
import com.petbuddy.petbuddystore.common.enums.CageStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.CageCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CageUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CageResponse;
import com.petbuddy.petbuddystore.mapper.CageMapper;
import com.petbuddy.petbuddystore.model.Cage;
import com.petbuddy.petbuddystore.repository.CageRepository;
import com.petbuddy.petbuddystore.service.CageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CageServiceImpl implements CageService {
    CageRepository cageRepository;
    CageMapper cageMapper;

    @Override
    public List<CageResponse> createCage(CageCreationRequest request) {
        String prefixCage = getPrefixCageSize(request.getCageSize());
        long currentCount = cageRepository.countByCageSize(request.getCageSize());
        List<Cage> cages = new ArrayList<>();

        for (int i = 0; i <= request.getQuantity(); i++) {
            String cageCode = prefixCage + String.format("%02d", currentCount + i);
            Cage cage = cageMapper.toCage(request);
            cage.setCageCode(cageCode);
            cage.setCageStatus(CageStatus.AVAILABLE);
            cages.add(cage);
        }
        return cageRepository.saveAll(cages)
                .stream()
                .map(cageMapper::toCageResponse)
                .toList();
    }

    @Override
    public Page<CageResponse> getCages(CageSize cageSize, CageStatus cageStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Cage> cagePage = cageRepository.findCages(cageSize, cageStatus, pageable);
        return cagePage.map(cageMapper::toCageResponse);
    }

    @Override
    public CageResponse getCageById(Long cageId) {
        Cage cage = cageRepository.findById(cageId)
                .orElseThrow(() -> new AppException(ErrorCode.CAGE_NOT_EXISTED));
        return cageMapper.toCageResponse(cage);
    }

    @Override
    public CageResponse updateCage(CageUpdateRequest request, Long cageId) {
        Cage cage = cageRepository.findById(cageId)
                .orElseThrow(() -> new AppException(ErrorCode.CAGE_NOT_EXISTED));
        cageMapper.updateCage(cage, request);
        return cageMapper.toCageResponse(cageRepository.save(cage));
    }

    private String getPrefixCageSize(CageSize cageSize) {
        return switch (cageSize) {
            case SMALL -> "S";
            case MEDIUM -> "M";
            case LARGE -> "L";
            case EXTRA_LARGE -> "EL";
        };
    }
}
