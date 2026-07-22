package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.AIConversationStatus;
import com.petbuddy.petbuddystore.model.AIConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, String> {
    Optional<AIConversation> findByConversationIdAndUserUserIdAndAiConversationStatus(
            String conversationId, String userId, AIConversationStatus aiConversationStatus);

    Page<AIConversation> findByUserUserIdAndAiConversationStatusOrderByUpdatedAtDesc(String userId, AIConversationStatus status,
                                                                                     Pageable pageable);
}
