package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionUpdateRequest {
    
    String name;
    String description;
    LocalDateTime startDate;
    LocalDateTime endDate;
    PromotionStatus status;
    List<PromotionDetailRequest> promotionDetails;
    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    String reason;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    String note;
}