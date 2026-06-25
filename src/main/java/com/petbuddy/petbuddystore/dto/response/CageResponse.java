package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.CageSize;
import com.petbuddy.petbuddystore.common.enums.CageStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;


import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CageResponse {
    Long cageId;
    String cageCode;
    CageSize cageSize;
    CageStatus cageStatus;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
