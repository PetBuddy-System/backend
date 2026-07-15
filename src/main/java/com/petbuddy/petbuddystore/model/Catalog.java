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
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column (name = "catalog_type", nullable = false)
    LocationType catalogType;

    @Column (name = "pet_species")
    String petSpecies;

    @Column(name = "prices", nullable = false)
    BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_range")
    WeightRange weightRange;

    @Column (name = "duration_minute")
    Integer durationMinute;

    @Column(name = "buffer_time")
    Integer bufferTime;

    @Column(name = "surcharge_config")
    String surchargeConfig;

    @Column (name = "status")
    @Enumerated (EnumType.STRING)
    CatalogStatus status;

    @Column (name = "created_at")
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column (name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "catalog", cascade = CascadeType.ALL, orphanRemoval = true)
    List<CatalogTimeSlot> catalogTimeSlots = new ArrayList<>();

}
