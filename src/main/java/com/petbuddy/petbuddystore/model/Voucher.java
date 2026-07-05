package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.ApplyScope;
import com.petbuddy.petbuddystore.common.enums.DiscountType;
import com.petbuddy.petbuddystore.common.enums.VoucherStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "voucher_id")
     UUID voucherId;

    @Column(name = "voucher_code", nullable = false, unique = true)
    @Size(min = 4, max = 20)
     String voucherCode;

    @Column(name = "voucher_name")
    @Size(max = 100)
     String voucherName;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
     DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
     BigDecimal discountValue;

    @Column(name = "max_discount", precision = 10, scale = 2)
     BigDecimal maxDiscount;

    @Column(name = "min_order_value", precision = 10, scale = 2)
     BigDecimal minOrderValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_scope")
     ApplyScope applyScope;

    @Column(name = "usage_limit")
    @Max(999999)
     Integer usageLimit;

    @Column(name = "used_count")
     Integer usedCount;

    @Column(name = "per_user_limit")
    @Min(1)
    @Max(1000)
     Integer perUserLimit;

    @Column(name = "start_at")
     LocalDateTime startAt;

    @Column(name = "end_at")
     LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
     VoucherStatus status;

    @Column(name = "created_at")
     LocalDateTime createdAt;

    @Column(name = "updated_at")
     LocalDateTime updatedAt;
}
