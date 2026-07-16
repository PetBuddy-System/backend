package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.common.enums.MediaPurpose;
import com.petbuddy.petbuddystore.dto.request.OrderReviewRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.OrderReviewResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.model.Review;
import com.petbuddy.petbuddystore.model.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ReviewMapper {

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reviewType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "anonymous", source = "anonymous")
    Review toEntity(ProductReviewCreationRequest request);

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reviewType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "anonymous", source = "anonymous")
    void updateEntity(@MappingTarget Review review, ProductReviewUpdateRequest request);

    @Mapping(target = "reviewId", source = "reviewId")
    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatar", expression = "java(getAvatarForUser(review))")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "anonymous", source = "anonymous")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    ProductReviewResponse toProductResponse(Review review);

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reviewType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "anonymous", source = "anonymous")
    Review toEntity(OrderReviewRequest request);

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reviewType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "anonymous", source = "anonymous")
    void updateEntity(@MappingTarget Review review, OrderReviewRequest request);

    @Mapping(target = "reviewId", source = "reviewId")
    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "orderCode", source = "order.orderCode")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatar", expression = "java(getAvatarForUser(review))")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "anonymous", source = "anonymous")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    OrderReviewResponse toOrderResponse(Review review);

    @Mapping(target = "reviewId", source = "reviewId")
    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productCode", source = "product.productCode")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "userAvatar", expression = "java(getAvatarForManager(review))")
    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "orderCode", source = "order.orderCode")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "anonymous", source = "anonymous")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "deletedAt", source = "deletedAt")
    ProductReviewManagerResponse toManagerResponse(Review review);

    @Mapping(target = "reviewId", source = "reviewId")
    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productCode", source = "product.productCode")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "userAvatar", expression = "java(getAvatarForManager(review))")
    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "orderCode", source = "order.orderCode")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "anonymous", source = "anonymous")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "deletedAt", source = "deletedAt")
    ProductReviewManagerResponse toManagerDetailResponse(Review review);

    default String getAvatarForUser(Review review) {
        if (review == null || review.getUser() == null) {
            return null;
        }
        if (Boolean.TRUE.equals(review.getAnonymous())) {
            return null;
        }
        return getAvatarFromMediaFiles(review.getUser());
    }

    default String getAvatarForManager(Review review) {
        if (review == null || review.getUser() == null) {
            return null;
        }
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