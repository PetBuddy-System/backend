package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.FileType;
import com.petbuddy.petbuddystore.common.enums.MediaPurpose;
import com.petbuddy.petbuddystore.common.enums.MediaStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.service.FileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FileServiceImpl implements FileService {
    S3Client s3Client;

    @NonFinal
    @Value("${aws.s3.bucket}")
    String bucketName;

    @Override
    public MediaFile uploadProductImage(MultipartFile file) {return uploadImage(file, MediaPurpose.PRODUCT, "products");}

    @Override
    public MediaFile uploadPetImage(MultipartFile file) {
        return uploadImage(file, MediaPurpose.PET_PROFILE, "pets");
    }

    @Override
    public MediaFile uploadBlogImage(MultipartFile file) {
        return uploadImage(file, MediaPurpose.BLOG, "blogs");
    }

    private MediaFile uploadImage(MultipartFile file, MediaPurpose mediaPurpose, String folder) {
        validateFile(file);

        try {
            String fileKey = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            String fileUrl = buildFileUrl(fileKey);
            return MediaFile.builder()
                    .fileUrl(fileUrl)
                    .fileKey(fileKey)
                    .fileSize(file.getSize())
                    .fileType(FileType.IMAGE)
                    .mediaPurpose(mediaPurpose)
                    .mediaStatus(MediaStatus.ACTIVE)
                    .build();

        } catch (Exception e) {
            log.error("Upload image failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    @Override
    public MediaFile uploadProductImageFromBytes(byte[] bytes) {
        String detectedMimeType = detectMimeType(bytes);
        if (detectedMimeType == null) {throw new AppException(ErrorCode.INVALID_FILE_TYPE);}

        validateBytes(bytes, detectedMimeType);
        String extension = getExtensionFromMimeType(detectedMimeType);
        String fileKey = "products/" + UUID.randomUUID() + "." + extension;
        String fileUrl = buildFileUrl(fileKey);

        uploadToS3(bytes, detectedMimeType, fileKey);

        return MediaFile.builder()
                .fileUrl(fileUrl)
                .fileKey(fileKey)
                .fileSize((long) bytes.length)
                .fileType(FileType.IMAGE)
                .mediaPurpose(MediaPurpose.PRODUCT)
                .mediaStatus(MediaStatus.ACTIVE)
                .build();
    }

    private String detectMimeType(byte[] data) {
        if (data == null || data.length < 4) return null;
        if (data[0] == (byte)0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data[0] == (byte)0xFF && data[1] == (byte)0xD8 && data[2] == (byte)0xFF) {
            return "image/jpeg";
        }
        if (data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46 &&
                data.length > 12 && data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50) {
            return "image/webp";
        }
        if (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x38) {
            return "image/gif";
        }
        return null;
    }    private String buildFileUrl(String fileKey){
        return "https://" + bucketName + ".s3.amazonaws.com/" + fileKey;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_REQUIRED);
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") &&
                !contentType.equals("image/webp") && !contentType.equals("image/jpg"))) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private void validateBytes(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            throw new AppException(ErrorCode.FILE_REQUIRED);
        }
        if (bytes.length > 5 * 1024 * 1024) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        if (mimeType == null || !mimeType.toLowerCase().startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String getExtensionFromMimeType(String mimeType) {
        String lower = mimeType.toLowerCase();
        if (lower.contains("png")) return "png";
        if (lower.contains("webp")) return "webp";
        if (lower.contains("gif")) return "gif";
        if (lower.contains("bmp")) return "bmp";
        return "jpg";
    }

    private String uploadToS3(byte[] bytes, String mimeType, String imageKey) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(imageKey)
                            .contentType(mimeType)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
            return "https://" + bucketName + ".s3.amazonaws.com/" + imageKey;
        } catch (Exception e) {
            log.error("Failed to upload bytes to S3: {}", e.getMessage());
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }
}