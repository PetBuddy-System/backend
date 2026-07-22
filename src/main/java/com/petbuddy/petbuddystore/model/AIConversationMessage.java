package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.AIMessageRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_conversation_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "conversation_message_id")
    String conversationMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AIMessageRole messageRole;

    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    AIConversation aiConversation;

    @OneToMany(mappedBy = "aiConversationMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<MediaFile> mediaFiles = new ArrayList<>();
}
