package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.StoreLocationRequest;
import com.petbuddy.petbuddystore.dto.response.StoreLocationResponse;

import java.util.List;

public interface StoreLocationService  {
    StoreLocationResponse createLocation(StoreLocationRequest request);
    StoreLocationResponse getCurrentLocation();
    List<StoreLocationResponse> getAllLocations();
    StoreLocationResponse getLocationById(Long id);
    StoreLocationResponse updateLocation(Long id, StoreLocationRequest request);
}
