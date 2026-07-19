package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.*;
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
@Setter
@Getter
@Table(name = "return_request")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnRequest {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long returnRequestId;

        @Column(unique = true, nullable = false)
        private String returnCode;

        // Đơn hàng cần hoàn/đổi
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "order_id", nullable = false)
        private Order order;

        // Người tạo yêu cầu
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "requested_by", nullable = false)
        private User requestedBy;

        // Coordinator xử lý yêu cầu
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "coordinator_id")
        private User coordinator;

        // Shipper được phân công
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "shipper_id")
        private User shipper;

        @Enumerated(EnumType.STRING)
        private ReturnType type;

        @Enumerated(EnumType.STRING)
        private ReturnReason reason;

        @Column(columnDefinition = "NVARCHAR(500)")
        private String description;

        @Enumerated(EnumType.STRING)
        private ReturnStatus status;

        @Enumerated(EnumType.STRING)
        private RefundMethod refundMethod;

        @Enumerated(EnumType.STRING)
        private RefundStatus refundStatus;

        // Tổng số tiền hoàn cuối cùng
        @Column(nullable = false, precision = 18, scale = 2)
        private BigDecimal refundAmount;

        // Ghi chú của staff
        @Column(columnDefinition = "NVARCHAR(500)")
        private String staffNote;

        private String bankName;
        private String bankAccountNumber;
        private String bankAccountHolder;

        @CreationTimestamp
        @Column(updatable = false)
        private LocalDateTime createdAt;

        private LocalDateTime approvedAt;

        private LocalDateTime pickedUpAt;

        private LocalDateTime returnedToStoreAt;

        private LocalDateTime completedAt;

        @UpdateTimestamp
        private LocalDateTime updatedAt;

        @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<ReturnItem> returnItems = new ArrayList<>();

        @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<MediaFile> mediaFiles = new ArrayList<>();

        @Column(name = "restocked_at")
        private LocalDateTime restockedAt;
}