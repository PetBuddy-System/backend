package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.CategoryStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponse {

    Long categoryId;

    String name;

    String description;

    CategoryStatus status;

    LocalDateTime deletedAt;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}