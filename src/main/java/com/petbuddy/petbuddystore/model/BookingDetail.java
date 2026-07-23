package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.WeightRange;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings_detail")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "booking_detail_id")
    Integer bookingDetailId;

    @Column(name = "catalog_name_snapshot")
    String catalogName;

    @Column(name = "catalog_type_snapshot")
    String catalogType; //DOG,CAT

    @Column(name = "pet_name_snapshot")
    String petName;

    @Column(name = "pet_species_snapshot")
    String petSpecies;

    @Column(name = "pet_weight_snapshot")
    BigDecimal petWeight;

    @Column(name = "pet_health_note_snapshot")
    String petHealthNote;

    @Column(name = "duration_minute_snapshot")
    Integer durationMinute;

    @Column(name = "base_duration_minute_snapshot")
    Integer baseDurationMinute;

    @Column(name = "additional_duration_minute_snapshot")
    Integer additionalDurationMinute;

    @Column(name = "total_duration_minute_snapshot")
    Integer totalDurationMinute;

    @Column(name = "unit_price_snapshot")
    BigDecimal unitPrice;

    @Column(name = "base_price_snapshot")
    BigDecimal basePrice;

    @Column(name = "additional_price_snapshot")
    BigDecimal additionalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_range_snapshot")
    WeightRange weightRange;

    @Column(name = "quantity")
    Integer quantity;

    @Column(name = "total_price")
    BigDecimal totalPrice;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "create_at")
    @CreationTimestamp
    LocalDateTime createAt;

    @OneToMany(mappedBy = "bookingDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<MediaFile> mediaFiles = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "booking_id")
    Booking booking;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    PetProfile pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id")
    Catalog catalog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id")
    CatalogTimeSlot timeSlot;



}
