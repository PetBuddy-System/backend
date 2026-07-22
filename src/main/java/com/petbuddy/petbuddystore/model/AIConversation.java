package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.AIConversationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "conversation_id")
    String conversationId;

    @Column(length = 200)
    String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AIConversationStatus aiConversationStatus;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @OneToMany(mappedBy = "aiConversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<AIConversationMessage> conversationMessages = new ArrayList<>();
}
