package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.PetStatus;
import com.petbuddy.petbuddystore.common.enums.VaccinationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pet_profiles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pet_id")
    String petId;

    @Column(name = "pet_name", nullable = false)
    String petName;

    @Column(nullable = false)
    String species;

    @Column(nullable = false)
    String breed;

    String gender;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    Double weight;

    String color;

    @Column(name = "health_note", columnDefinition = "TEXT")
    String healthNote;

    @Column(name = "allergy_note", columnDefinition = "TEXT")
    String allergyNote;

    @Column(name = "behavior_note", columnDefinition = "TEXT")
    String behaviorNote;

    @Column(name = "vaccination_status")
    @Enumerated(EnumType.STRING)
    VaccinationStatus vaccinationStatus;

    @Enumerated(EnumType.STRING)
    PetStatus petStatus;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @OneToMany(mappedBy = "petProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MediaFile> mediaFiles = new ArrayList<>();
}
