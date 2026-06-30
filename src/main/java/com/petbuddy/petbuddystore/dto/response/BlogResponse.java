package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.model.MediaFile;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogResponse {
    String blogId;
    String userId;
    String title;
    String label;
    String snippet;
    String content;
    List<MediaFileResponse> mediaFiles;
    LocalDateTime createdAt;
}