package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Booking {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)

    @Column(name = "booking_id")
    Integer bookingId;

    @Column(name = "booking_code")
    String bookingCode;

    @Column(name = "booking_type")
    String bookingType;

    @Column(name = "customer_name_snapshot")
    String customerName;

    @Column(name = "customer_phone_snapshot")
    String customerPhone;

    @Column(name = "address_snapshot")
    String address;

    @Column(name = "shipping_fee")
    BigDecimal shippingFee;


    @Column(name = "scheduled_at", nullable = false)
    LocalDateTime scheduledAt;

    @Column(name = "total_amount")
    BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    BookingStatus bookingStatus;

    @Column(name = "cancel_deadline_at")
    LocalDateTime cancelDeadlineAt;

    @Column(name = "need_pet_shipping")
    Boolean isShipping;

    @Column(name = "create_at")
    @CreationTimestamp
    LocalDateTime createAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @Builder.Default
    List<BookingDetail> bookingDetails = new ArrayList<>();







}
