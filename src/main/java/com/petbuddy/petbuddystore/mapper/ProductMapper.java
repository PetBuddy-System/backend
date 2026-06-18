package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.ProductCreationRequest;
import com.petbuddy.petbuddystore.dto.response.ProductDetailResponse;
import com.petbuddy.petbuddystore.dto.response.ProductManagementResponse;
import com.petbuddy.petbuddystore.dto.response.ProductPublicResponse;
import com.petbuddy.petbuddystore.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    Product toProduct(ProductCreationRequest request);

    ProductPublicResponse toPublicResponse(Product product);

    ProductManagementResponse toManagementResponse(Product product);

    ProductDetailResponse toDetailResponse(Product product);
}