package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.ProductBatchCreationRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBatchResponse;
import com.petbuddy.petbuddystore.model.ProductBatch;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductBatchMapper {

    ProductBatch toProductBatch(ProductBatchCreationRequest request);

    ProductBatchResponse toProductBatchResponse(ProductBatch productBatch);
}