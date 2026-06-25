package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.CageSize;
import com.petbuddy.petbuddystore.common.enums.CageStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cages")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cage_id")
    Long cageId;

    @Column(name = "cage_code", unique = true, nullable = false)
    String cageCode;

    @Enumerated(EnumType.STRING)
    CageSize cageSize;

    @Enumerated(EnumType.STRING)
    CageStatus cageStatus;

    String note;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

}