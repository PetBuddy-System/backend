package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatAIRequest {
    @Size(max = 2000, message = "MESSAGE_EXCEED_LIMIT")
    String message;
    List<String> imageUrls;
}
