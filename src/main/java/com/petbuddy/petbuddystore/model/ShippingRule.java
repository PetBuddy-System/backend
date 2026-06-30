package com.petbuddy.petbuddystore.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Setter
@Getter
@Table(name = "shipping_rules")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShippingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Double minDistance;
    Double maxDistance;
    BigDecimal fee;
}
