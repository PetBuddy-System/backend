package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.CatalogStatus;
import com.petbuddy.petbuddystore.common.enums.WeightRange;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults (level = AccessLevel.PRIVATE)
public class CatalogUpdateRequest {
    String catalogName;
    String description;
    String catalogType;
    String petSpecies;
    BigDecimal price;
    WeightRange weightRange;
    Integer durationMinute;
    Integer bufferTime;
    CatalogStatus status;
    String surchargeConfig;
}
