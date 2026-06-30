package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "promotion_id")
    UUID promotionId;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    String name;

    @Column(columnDefinition = "NVARCHAR(1000)")
    String description;

    @Column(nullable = false)
    LocalDateTime startDate;

    @Column(nullable = false)
    LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    PromotionStatus status = PromotionStatus.DRAFT;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @OneToMany(mappedBy = "promotion",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    List<PromotionDetail> promotionDetails = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}