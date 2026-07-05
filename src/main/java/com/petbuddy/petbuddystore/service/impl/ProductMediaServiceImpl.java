package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.FileType;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.response.ProductMediaResponse;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.repository.MediaFileRepository;
import com.petbuddy.petbuddystore.repository.ProductRepository;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.ProductMediaService;
import com.petbuddy.petbuddystore.service.ProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductMediaServiceImpl implements ProductMediaService {

    ProductService productService;
    FileService fileService;
    MediaFileRepository mediaFileRepository;
    ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductMediaResponse> getProductImages(UUID productId) {
        Product product = productService.getProductEntityById(productId);
        return product.getMediaFiles().stream()
                .filter(media -> media.getFileType() == FileType.IMAGE)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductMediaResponse getProductVideo(UUID productId) {
        Product product = productService.getProductEntityById(productId);
        
        return product.getMediaFiles().stream()
                .filter(media -> media.getFileType() == FileType.VIDEO)
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public void updateProductImages(UUID productId, List<MultipartFile> images, List<Long> keepImageIds) {
        if (images == null && keepImageIds == null) {return;}

        Product product = productService.getProductEntityById(productId);

        List<MediaFile> currentImages = product.getMediaFiles().stream()
                .filter(media -> media.getFileType() == FileType.IMAGE)
                .toList();

        long keepCount = 0;
        if (keepImageIds != null && !keepImageIds.isEmpty()) {
            keepCount = currentImages.stream()
                    .filter(media -> keepImageIds.contains(media.getMediaFileId()))
                    .count();
        }

        long newImagesCount = images != null ? images.size() : 0;
        long totalAfterUpdate = keepCount + newImagesCount;

        if (totalAfterUpdate > 4) {throw new AppException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED);}

        // Xóa ảnh: giữ lại những ảnh có ID trong keepImageIds
        if (keepImageIds != null) {
            List<MediaFile> imagesToDelete = currentImages.stream()
                    .filter(media -> !keepImageIds.contains(media.getMediaFileId()))
                    .toList();

            if (!imagesToDelete.isEmpty()) {
                mediaFileRepository.deleteAll(imagesToDelete);
                product.getMediaFiles().removeAll(imagesToDelete);
            }
        } else {
            // Nếu keepImageIds = null => xóa hết ảnh cũ
            if (!currentImages.isEmpty()) {
                mediaFileRepository.deleteAll(currentImages);
                product.getMediaFiles().removeAll(currentImages);
            }
        }

        // Thêm ảnh mới
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    MediaFile mediaFile = fileService.uploadProductImage(image);
                    mediaFile.setProduct(product);
                    mediaFileRepository.save(mediaFile);
                }
            }
        }
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void updateProductVideo(UUID productId, MultipartFile video) {
        // video = null: KHÔNG làm gì
        if (video == null) {
            return;
        }

        Product product = productService.getProductEntityById(productId);

        // Xóa video cũ
        product.getMediaFiles().stream()
                .filter(media -> media.getFileType() == FileType.VIDEO)
                .findFirst()
                .ifPresent(oldVideo -> {
                    mediaFileRepository.delete(oldVideo);
                    product.getMediaFiles().remove(oldVideo);
                });

        // Nếu có file mới thì upload (chỉ 1 video)
        if (!video.isEmpty()) {
            MediaFile newVideo = fileService.uploadProductVideo(video);
            newVideo.setProduct(product);
            mediaFileRepository.save(newVideo);
        }
        // video.isEmpty() => chỉ xóa, không upload

        productRepository.save(product);
    }

    private ProductMediaResponse toResponse(MediaFile mediaFile) {
        return ProductMediaResponse.builder()
                .mediaFileId(mediaFile.getMediaFileId())
                .fileUrl(mediaFile.getFileUrl())
                .fileType(mediaFile.getFileType())
                .build();
    }
}