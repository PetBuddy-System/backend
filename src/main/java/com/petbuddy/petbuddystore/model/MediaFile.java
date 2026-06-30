package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.FileType;
import com.petbuddy.petbuddystore.common.enums.MediaPurpose;
import com.petbuddy.petbuddystore.common.enums.MediaStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_file_id")
    Long mediaFileId;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    String fileUrl;

    @Column(name = "file_key", nullable = false, unique = true)
    String fileKey;

    @Column(name = "file_size")
    Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    FileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_purpose", nullable = false)
    MediaPurpose mediaPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_status")
    MediaStatus mediaStatus;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    PetProfile petProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id")
    Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    Product product;
}
