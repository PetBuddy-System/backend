package com.petbuddy.petbuddystore.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    String catalogType;

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

    @Column(name = "unit_price_snapshot")
    BigDecimal unitPrice;

    @Column(name = "quantity")
    Integer quantity;

    @Column(name = "total_price")
    BigDecimal totalPrice;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "create_at")
    @CreationTimestamp
    LocalDateTime createAt;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    Booking booking;

    @ManyToOne
    @JoinColumn(name = "cage_id")
    Cage cage;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    Pet pet;

    @ManyToOne
    @JoinColumn(name = "catalog_id")
    Catalog catalog;

}
