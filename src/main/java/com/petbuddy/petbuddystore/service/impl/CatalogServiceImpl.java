package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.CatalogStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.CatalogCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CatalogUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CatalogResponse;
import com.petbuddy.petbuddystore.mapper.CatalogMapper;
import com.petbuddy.petbuddystore.model.Catalog;
import com.petbuddy.petbuddystore.repository.CatalogRepository;
import com.petbuddy.petbuddystore.service.CatalogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogServiceImpl implements CatalogService {
     CatalogRepository catalogRepository;
     CatalogMapper catalogMapper;


    @Override
    public List<CatalogResponse> getAllCatalogs() {
        return catalogRepository.findAll()
                .stream()
                .map(catalogMapper::toCatalogResponse)
                .toList();
    }


    @Override
    public CatalogResponse createCatalog(CatalogCreationRequest request) {
        Catalog catalog = catalogMapper.toCatalog(request);

        // Thiết lập trạng thái mặc định nếu không truyền lên
        if (catalog.getStatus() == null) {
            catalog.setStatus(CatalogStatus.AVAILABLE);
        }
        return catalogMapper.toCatalogResponse(catalogRepository.save(catalog));
    }

    @Override
    public CatalogResponse getCatalogById(int catalogId) {
        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_NOT_FOUND));
        return catalogMapper.toCatalogResponse(catalog);
    }

    @Override
    public CatalogResponse updateCatalog(int catalogId, CatalogUpdateRequest updateRequest) {
        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_NOT_FOUND));
        catalogMapper.updateCatalog(updateRequest, catalog);
        return catalogMapper.toCatalogResponse(catalogRepository.save(catalog));
    }

    @Override
    public CatalogResponse updateCatalogStatus(Integer catalogId, CatalogStatus status) {
        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_NOT_FOUND));

        catalog.setStatus(status);
        return catalogMapper.toCatalogResponse(catalogRepository.save(catalog));
    }
}
