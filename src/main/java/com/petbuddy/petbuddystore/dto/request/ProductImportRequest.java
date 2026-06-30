package com.petbuddy.petbuddystore.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportRequest {
    private MultipartFile file;
    private boolean confirm;
}