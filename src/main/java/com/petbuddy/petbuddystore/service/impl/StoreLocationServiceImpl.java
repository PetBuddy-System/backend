package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.StoreLocationRequest;
import com.petbuddy.petbuddystore.dto.response.StoreLocationResponse;
import com.petbuddy.petbuddystore.mapper.StoreLocationMapper;
import com.petbuddy.petbuddystore.model.StoreLocation;
import com.petbuddy.petbuddystore.repository.StoreLocationRepository;
import com.petbuddy.petbuddystore.service.ShippingRuleService;
import com.petbuddy.petbuddystore.service.StoreLocationService ;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoreLocationServiceImpl implements StoreLocationService  {

    StoreLocationRepository storeLocationRepository;
    StoreLocationMapper storeLocationMapper;

    ShippingRuleService  shippingRuleService;

    @Override
    @Transactional
    public StoreLocationResponse createLocation(StoreLocationRequest request) {
        shippingRuleService.validateLocation(request.getLatitude(), request.getLongitude());

        storeLocationRepository.findByActiveTrue().ifPresent(current -> {
            current.setActive(false);
            current.setDeactivatedAt(LocalDateTime.now());
            storeLocationRepository.save(current);
        });

        StoreLocation newLocation = storeLocationMapper.toEntity(request);
        newLocation.setActive(true);

        return storeLocationMapper.toResponse(storeLocationRepository.save(newLocation));
    }

    @Override
    @Transactional
    public StoreLocationResponse updateLocation(Long id, StoreLocationRequest request) {
        shippingRuleService.validateLocation(request.getLatitude(), request.getLongitude());

        StoreLocation location = storeLocationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setAddress(request.getAddress());

        return storeLocationMapper.toResponse(storeLocationRepository.save(location));
    }

    @Override
    public StoreLocationResponse getCurrentLocation() {
        StoreLocation current = storeLocationRepository.findByActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));
        return storeLocationMapper.toResponse(current);
    }

    @Override
    public List<StoreLocationResponse> getAllLocations() {
        return storeLocationMapper.toResponseList(
                storeLocationRepository.findAllByOrderByCreatedAtDesc());
    }

    @Override
    public StoreLocationResponse getLocationById(Long id) {
        StoreLocation location = storeLocationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));
        return storeLocationMapper.toResponse(location);
    }
}
