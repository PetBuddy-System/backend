package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.dto.request.ChatAIRequest;
import com.petbuddy.petbuddystore.service.ChatAIService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Chatbot API", description = "Chatbot AI")
@Slf4j
public class ChatAIController {
    ChatAIService chatAIService;

    @PostMapping("/chat")
    String chat(@RequestBody ChatAIRequest chatAIRequest) {
        return chatAIService.chat(chatAIRequest);
    }

}
