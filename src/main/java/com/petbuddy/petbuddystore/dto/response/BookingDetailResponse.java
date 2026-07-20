package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.WeightRange;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetailResponse {
    Integer bookingDetailId;
    String petId;
    String petName;
    String petImage;
    String petSpecies;
    BigDecimal petWeight;
    String petHealthNote;
    Integer catalogId;
    String catalogName;
    String catalogImage;
    String catalogType;
    Integer timeSlotId;
    String timeSlot;
    Integer durationMinute;
    BigDecimal unitPrice;
    WeightRange weightRange;
    Integer baseDurationMinute;
    Integer additionalDurationMinute;
    Integer totalDurationMinute;
    BigDecimal basePrice;
    BigDecimal additionalPrice;
    Integer quantity;
    BigDecimal totalPrice;
    String note;
    List<MediaFileResponse> mediaFiles;
}
