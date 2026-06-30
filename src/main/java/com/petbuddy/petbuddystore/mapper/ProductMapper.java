package com.petbuddy.petbuddystore.mapper;

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
    @Mapping(target = "promotionId", ignore = true)
    @Mapping(target = "promotionName", ignore = true)
    @Mapping(target = "discountType", ignore = true)
    @Mapping(target = "discountValue", ignore = true)
    @Mapping(target = "salePrice", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    @Mapping(target = "promotionEndDate", ignore = true)
    @Mapping(target = "hasActivePromotion", ignore = true)
    @Mapping(target = "imageUrls", expression = "java(mapMediaFilesToUrls(product.getMediaFiles()))")
    ProductPublicResponse toPublicResponse(Product product);

    @Mapping(target = "description", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "usageInstructions", ignore = true)
    @Mapping(target = "discountType", ignore = true)
    @Mapping(target = "discountValue", ignore = true)
    @Mapping(target = "salePrice", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    @Mapping(target = "promotionEndDate", ignore = true)
    @Mapping(target = "hasActivePromotion", ignore = true)
    @Mapping(target = "imageUrls", expression = "java(mapMediaFilesToUrls(product.getMediaFiles()))")
    ProductManagementResponse toManagementResponse(Product product);

    @Mapping(target = "imageUrls", expression = "java(mapMediaFilesToUrls(product.getMediaFiles()))")
    ProductPublicResponse toDetailPublicResponse(Product product);

    default List<String> mapMediaFilesToUrls(List<MediaFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaFiles.stream()
                .map(MediaFile::getFileUrl)
                .collect(Collectors.toList());
    }
}