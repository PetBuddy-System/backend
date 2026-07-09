package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import com.petbuddy.petbuddystore.model.ProductReview;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductReviewMapper {

    ProductReview toEntity(ProductReviewCreationRequest request);

    void updateEntity(@MappingTarget ProductReview review, ProductReviewUpdateRequest request);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    ProductReviewResponse toResponse(ProductReview review);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productCode", source = "product.productCode")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    ProductReviewManagerResponse toManagerResponse(ProductReview review);
}