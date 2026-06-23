package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public String uploadProductImage(MultipartFile file) {
        return uploadToS3(file, "products");
    }

    @Override
    public String uploadPetImage(MultipartFile file) {
        return uploadToS3(file, "pets");
    }

    @Override
    public String uploadBlogImage(MultipartFile file) {
        return uploadToS3(file, "blogs");
    }

    private String uploadToS3(MultipartFile file, String folder) {
        validateFile(file);

        try {
            String imageKey = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(imageKey)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            return "https://" + bucketName + ".s3.amazonaws.com/" + imageKey;

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
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
}