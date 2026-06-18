package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.ApplyScope;
import com.petbuddy.petbuddystore.common.enums.DiscountType;
import com.petbuddy.petbuddystore.common.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "voucher")
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "voucher_id")
    private UUID voucherId;

    @Column(name = "voucher_code", nullable = false, unique = true)
    private String voucherCode;

    @Column(name = "voucher_name")
    private String voucherName;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_value", precision = 10, scale = 2)
    private BigDecimal minOrderValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_scope")
    private ApplyScope applyScope;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count")
    private Integer usedCount;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private VoucherStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
