package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.common.enums.MediaPurpose;
import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.model.ProductReview;
import com.petbuddy.petbuddystore.model.User;
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
    @Mapping(target = "avatar", expression = "java(getAvatarForUser(review))")
    ProductReviewResponse toResponse(ProductReview review);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productCode", source = "product.productCode")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    ProductReviewManagerResponse toManagerResponse(ProductReview review);


    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productCode", source = "product.productCode")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "userAvatar", expression = "java(getAvatarForManager(review))")
    ProductReviewManagerResponse toManagerDetailResponse(ProductReview review);

    default String getAvatarForUser(ProductReview review) {
        if (review == null || review.getUser() == null) {
            return null;
        }
        if (Boolean.TRUE.equals(review.getAnonymous())) {
            return null;
        }

        return getAvatarFromMediaFiles(review.getUser());
    }

    default String getAvatarForManager(ProductReview review) {
        if (review == null || review.getUser() == null) {
            return null;
        }

        // Manager: luôn hiển thị avatar, kể cả anonymous
        return getAvatarFromMediaFiles(review.getUser());
    }

    private String getAvatarFromMediaFiles(User user) {
        if (user.getMediaFiles() == null || user.getMediaFiles().isEmpty()) {
            return null;
        }
        return user.getMediaFiles().stream()
                .filter(media -> media.getMediaPurpose() == MediaPurpose.USER_PROFILE)
                .findFirst()
                .map(MediaFile::getFileUrl)
                .orElse(null);
    }
}