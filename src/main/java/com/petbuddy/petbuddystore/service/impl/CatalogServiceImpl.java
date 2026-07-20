package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.CatalogStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.CatalogCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CatalogUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CatalogResponse;
import com.petbuddy.petbuddystore.mapper.CatalogMapper;
import com.petbuddy.petbuddystore.model.Catalog;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.repository.CatalogRepository;
import com.petbuddy.petbuddystore.service.CatalogService;
import com.petbuddy.petbuddystore.service.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogServiceImpl implements CatalogService {
     CatalogRepository catalogRepository;
     CatalogMapper catalogMapper;
     FileService fileService;


    @Override
    public List<CatalogResponse> getAllCatalogs() {
        return catalogRepository.findAll()
                .stream()
                .map(catalogMapper::toCatalogResponse)
                .toList();
    }


    @Override
    public CatalogResponse createCatalog(CatalogCreationRequest request) {
        validateCatalogPricingConfig(request.getAdditionalDurationConfig(), request.getAdditionalPricePerMinute());
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
        validateCatalogPricingConfig(updateRequest.getAdditionalDurationConfig(), updateRequest.getAdditionalPricePerMinute());
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

    @Override
    public CatalogResponse uploadCatalogImage(Integer catalogId, MultipartFile file) {
        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_NOT_FOUND));
        String oldImageUrl = catalog.getImageUrl();
        MediaFile mediaFile = fileService.uploadCatalogImage(file);
        catalog.setImageUrl(mediaFile.getFileUrl());
        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            fileService.deleteFile(oldImageUrl);
        }
        return catalogMapper.toCatalogResponse(catalogRepository.save(catalog));
    }

    @Override
    public CatalogResponse deleteCatalogImage(Integer catalogId) {
        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_NOT_FOUND));
        if (catalog.getImageUrl() != null && !catalog.getImageUrl().isBlank()) {
            fileService.deleteFile(catalog.getImageUrl());
        }
        catalog.setImageUrl(null);
        return catalogMapper.toCatalogResponse(catalogRepository.save(catalog));
    }

    private void validateCatalogPricingConfig(String additionalDurationConfig, BigDecimal additionalPricePerMinute) {
        if (additionalPricePerMinute != null && additionalPricePerMinute.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_ADDITIONAL_PRICE_PER_MINUTE);
        }
        if (additionalDurationConfig == null || additionalDurationConfig.isBlank()) {
            return;
        }
        for (String part : additionalDurationConfig.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) {
                throw new AppException(ErrorCode.INVALID_ADDITIONAL_DURATION_CONFIG);
            }
            try {
                com.petbuddy.petbuddystore.common.enums.WeightRange.valueOf(kv[0].trim().toUpperCase());
                int minutes = Integer.parseInt(kv[1].trim());
                if (minutes < 0) {
                    throw new AppException(ErrorCode.INVALID_ADDITIONAL_DURATION_CONFIG);
                }
            } catch (IllegalArgumentException ex) {
                throw new AppException(ErrorCode.INVALID_ADDITIONAL_DURATION_CONFIG);
            }
        }
    }
}
