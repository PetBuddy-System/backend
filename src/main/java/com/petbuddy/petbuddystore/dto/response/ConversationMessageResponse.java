package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.AIMessageRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationMessageResponse {
    String conversationMessageId;
    AIMessageRole role;
    String content;
    List<MediaFileResponse> mediaFiles;
    LocalDateTime createdAt;
}
