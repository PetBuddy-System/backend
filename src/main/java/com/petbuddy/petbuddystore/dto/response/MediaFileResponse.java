package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.FileType;
import com.petbuddy.petbuddystore.common.enums.MediaPurpose;
import com.petbuddy.petbuddystore.common.enums.MediaStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaFileResponse {
    Long mediaFileId;
    String fileUrl;
    String fileKey;
    Long fileSize;
    FileType fileType;
    MediaPurpose mediaPurpose;
    MediaStatus mediaStatus;
}
