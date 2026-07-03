package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.common.enums.FileType;
import com.petbuddy.petbuddystore.dto.request.ProductCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductManagementResponse;
import com.petbuddy.petbuddystore.dto.response.ProductPublicResponse;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.model.Product;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {
    void updateProduct(@MappingTarget Product product, ProductUpdateRequest request);

    Product toProduct(ProductCreationRequest request);

    @Mapping(target = "description", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "usageInstructions", ignore = true)
    @Mapping(target = "promotionType", ignore = true)
    @Mapping(target = "discountValue", ignore = true)
    @Mapping(target = "salePrice", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    @Mapping(target = "promotionEndDate", ignore = true)
    @Mapping(target = "hasActivePromotion", ignore = true)
    ProductManagementResponse toManagementResponse(Product product);

    @Mapping(target = "description", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "usageInstructions", ignore = true)
    @Mapping(target = "totalStock", ignore = true)
    ProductPublicResponse toListResponse(Product product);

    ProductPublicResponse toDetailPublicResponse(Product product);
}