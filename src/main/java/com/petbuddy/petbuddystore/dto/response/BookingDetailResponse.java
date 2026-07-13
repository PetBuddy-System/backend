package com.petbuddy.petbuddystore.dto.response;

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
    String petSpecies;
    BigDecimal petWeight;
    String petHealthNote;
    Integer catalogId;
    String catalogName;
    String catalogType;
    Integer timeSlotId;
    String timeSlot;
    Integer durationMinute;
    BigDecimal unitPrice;
    Integer quantity;
    BigDecimal totalPrice;
    String note;
    List<MediaFileResponse> mediaFiles;
}
