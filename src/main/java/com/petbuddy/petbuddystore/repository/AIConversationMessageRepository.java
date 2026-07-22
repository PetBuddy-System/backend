package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.AIConversation;
import com.petbuddy.petbuddystore.model.AIConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIConversationMessageRepository extends JpaRepository<AIConversationMessage, String> {
    List<AIConversationMessage> findTop20ByAiConversationOrderByCreatedAtDesc(AIConversation aiConversation);
    List<AIConversationMessage> findByAiConversationOrderByCreatedAtAsc(AIConversation conversation);
}
