package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryUpdateRequest {

    String name;

    String description;

    CategoryStatus status;
}