package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.CatalogStatus;
import com.petbuddy.petbuddystore.dto.request.CatalogCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CatalogUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CatalogResponse;


import java.util.List;


public interface CatalogService {
    List<CatalogResponse> getAllCatalogs();
    CatalogResponse createCatalog(CatalogCreationRequest request);
    CatalogResponse getCatalogById(int catalogId);
    CatalogResponse updateCatalog(int catalogId, CatalogUpdateRequest updateRequest);
    CatalogResponse updateCatalogStatus(Integer catalogId, CatalogStatus status);
}
