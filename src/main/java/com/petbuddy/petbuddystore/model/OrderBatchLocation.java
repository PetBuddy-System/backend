package com.petbuddy.petbuddystore.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "order_batch_location")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderBatchLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "order_detail_id")
    OrderDetail orderDetail;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    ProductBatch batch;

    Integer quantity;
}
