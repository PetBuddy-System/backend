package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.CatalogStatus;
import com.petbuddy.petbuddystore.common.enums.WeightRange;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CatalogResponse {
    Integer catalogId;
    String catalogName;
    String description;
    String catalogType;
    String petSpecies;
    BigDecimal price;
    WeightRange weightRange;
    Integer durationMinute;
    Integer bufferTime;
    CatalogStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
