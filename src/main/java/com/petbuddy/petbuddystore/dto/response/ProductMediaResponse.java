package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.FileType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductMediaResponse {
    Long mediaFileId;
    String fileUrl;
    FileType fileType;
}