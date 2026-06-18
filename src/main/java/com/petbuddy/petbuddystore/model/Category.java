package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    Long categoryId;

    @Column(nullable = false, unique = true, columnDefinition = "NVARCHAR(100)")
    String name;

    @Column(columnDefinition = "NVARCHAR(500)")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    CategoryStatus status = CategoryStatus.ACTIVE;

    LocalDateTime deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "category")
    List<Product> products = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}