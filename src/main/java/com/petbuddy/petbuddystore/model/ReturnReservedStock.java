package com.petbuddy.petbuddystore.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_reserved_stock")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnReservedStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "return_item_id")
    ReturnItem returnItem;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    ProductBatch batch;

    @Column(name = "restocked_quantity", nullable = true)
    Integer restockedQuantity;

    @Column(name = "restocked_at", nullable = true)
    LocalDateTime restockedAt;

    @Column(name = "restocked_by", nullable = true)
    String restockedBy;

    Integer quantity;

    LocalDateTime reservedAt;

    LocalDateTime expiredAt;  // Hết hạn giữ hàng (24h)
}