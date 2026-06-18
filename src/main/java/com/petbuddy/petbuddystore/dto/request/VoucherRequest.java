package com.petbuddy.petbuddystore.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class VoucherRequest {
    String voucherCode;
    String voucherName;
    String discountType;
    BigDecimal discountValue;
    BigDecimal maxDiscount;
    BigDecimal minOrderValue;
    String applyScope;
    Integer usageLimit;
    Integer perUserLimit;
    LocalDateTime startAt;
    LocalDateTime expiredAt;
    String status;
}
