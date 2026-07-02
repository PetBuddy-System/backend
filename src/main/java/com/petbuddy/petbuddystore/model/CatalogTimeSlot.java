package com.petbuddy.petbuddystore.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(
        name = "catalog_time_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_catalog_day_start_time",
                columnNames = {"catalog_id", "day_of_week", "start_time"}
        )
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CatalogTimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_slot_id")
    Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    LocalTime startTime;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id", nullable = false)
    Catalog catalog;

}
