package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.ApplyScope;
import com.petbuddy.petbuddystore.common.enums.DiscountType;
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
    DiscountType discountType;
    BigDecimal discountValue;
    BigDecimal maxDiscount;
    BigDecimal minOrderValue;
    ApplyScope applyScope;
    Integer usageLimit;
    Integer perUserLimit;
    LocalDateTime startAt;
    LocalDateTime expiredAt;
    String status;
}
