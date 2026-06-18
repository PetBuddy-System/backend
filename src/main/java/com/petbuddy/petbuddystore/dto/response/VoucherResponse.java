package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherResponse {
     UUID voucherId;
     String voucherCode;
     String voucherName;
     String discountType;
     BigDecimal discountValue;
     BigDecimal maxDiscount;
     BigDecimal minOrderValue;
     String applyScope;
     Integer usageLimit;
     Integer usedCount;
     Integer perUserLimit;
     LocalDateTime startAt;
     LocalDateTime expiredAt;
     String status;
     LocalDateTime createdAt;
}
