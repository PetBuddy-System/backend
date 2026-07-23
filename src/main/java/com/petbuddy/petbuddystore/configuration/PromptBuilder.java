package com.petbuddy.petbuddystore.configuration;

import com.petbuddy.petbuddystore.common.enums.AIMessageRole;
import com.petbuddy.petbuddystore.model.AIConversationMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
public class PromptBuilder {
    SystemPrompt systemPrompt;

    public List<Message> buildMessages(List<AIConversationMessage> historyMessages,
            String currentUserMessage, List<Media> currentMedia) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt.getPrompt()));

        if (historyMessages != null) {
            for (AIConversationMessage historyMessage : historyMessages) {
                String content = historyMessage.getContent();
                if (content == null || content.isBlank()) {
                    continue;
                }

                switch (historyMessage.getMessageRole()) {
                    case USER -> messages.add(new UserMessage(content));
                    case ASSISTANT -> messages.add(new AssistantMessage(content));
                    default -> {}
                }
            }
        }

        UserMessage currentMessage = UserMessage.builder()
                .text(currentUserMessage == null ? "" : currentUserMessage)
                .media(currentMedia == null ? List.of() : currentMedia)
                .build();

        messages.add(currentMessage);
        return messages;
    }
}
