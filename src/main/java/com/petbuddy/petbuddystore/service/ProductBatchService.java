package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.dto.request.ProductBatchCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductBatchUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.ProductImportRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBatchResponse;
import com.petbuddy.petbuddystore.dto.response.ProductImportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductBatchService {

    List<ProductBatchResponse> createBatches(UUID productId, List<ProductBatchCreationRequest> requests);

    Page<ProductBatchResponse> getBatchesByProduct(UUID productId, String keyword, ProductStatus status, String sortBy, Pageable pageable);

    ProductBatchResponse updateBatch(UUID batchId, ProductBatchUpdateRequest request);

    void deleteDeletedBatchesOlderThan90Days();

    ProductImportResponse importProductsAndBatches(MultipartFile file);
}