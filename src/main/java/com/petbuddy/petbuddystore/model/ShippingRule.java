package com.petbuddy.petbuddystore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Setter
@Getter
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
