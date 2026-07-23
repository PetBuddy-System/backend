package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.CageStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CageUpdateRequest {
    CageStatus cageStatus;
    String note;
}
