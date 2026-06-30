package com.petbuddy.petbuddystore.model;


import com.petbuddy.petbuddystore.common.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table (name = "catalogs")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Catalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "catalog_id")
    Integer catalogId;

    @Column (name = "catalog_name")
    @NotNull
    String catalogName;

    @Column (name = "description")
    String description;

    @Column (name = "catalog_type", nullable = false)
    String catalogType;

    @Column (name = "pet_species")
    String petSpecies;

    @Column(name = "base_price", nullable = false)
    BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_range")
    WeightRange weightRange;

    @Column (name = "duration_minute")
    Integer durationMinute;

    @Column (name = "status")
    @Enumerated (EnumType.STRING)
    CatalogStatus status;

    @Column (name = "created_at")
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column (name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

}
