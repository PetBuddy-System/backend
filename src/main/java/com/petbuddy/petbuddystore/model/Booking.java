package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import com.petbuddy.petbuddystore.common.enums.StaffAssignmentMode;
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

    @Version
    @Column(name = "version")
    Long version;

    @Column(name = "booking_code")
    String bookingCode;

    @Column(name = "booking_type")
    String bookingType; //AT_HOME, AT_STORE

    @Column(name = "customer_name_snapshot")
    String customerName;

    @Column(name = "customer_phone_snapshot")
    String customerPhone;

    @Column(name = "address_snapshot")
    String address;

    @Column(name = "scheduled_at", nullable = false)
    LocalDateTime scheduledAt;

    @Column(name = "total_amount")
    BigDecimal totalAmount;

    @Column(name = "deposit_amount")
    BigDecimal depositAmount;

    @Column(name = "remaining_amount")
    BigDecimal remainingAmount;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "cancel_reason")
    String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    BookingStatus bookingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_mode")
    StaffAssignmentMode assignmentMode;

    @Column(name = "requested_staff_id")
    String requestedStaffId;

    @Column(name = "last_auto_reassign_at")
    LocalDateTime lastAutoReassignAt;

    @Column(name = "payment_deadline_at")
    LocalDateTime paymentDeadlineAt; //hạn thanh toán cọc

    @Column(name = "cancel_deadline_at")
    LocalDateTime cancelDeadlineAt;

    @Column(name = "create_at")
    @CreationTimestamp
    LocalDateTime createAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @Builder.Default
    List<BookingDetail> bookingDetails = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Payment> payments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_schedule_id")
    StaffSchedule staffSchedule;









}
