package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.ChatAIRequest;
import com.petbuddy.petbuddystore.dto.response.ChatAIResponse;
import com.petbuddy.petbuddystore.dto.response.ConversationDetailResponse;
import com.petbuddy.petbuddystore.dto.response.ConversationSummaryResponse;
import com.petbuddy.petbuddystore.service.ChatAIService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Chatbot API", description = "Chatbot AI")
@Slf4j
public class ChatAIController {
    ChatAIService chatAIService;

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChatAIResponse>> chat(@RequestParam(value = "conversationId", required = false) String conversationId,
                                                            @RequestParam(value = "message", required = false) String message,
                                                            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        ChatAIRequest request = ChatAIRequest.builder()
                .conversationId(conversationId)
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("AI prompt successfully", chatAIService.chat(request, images)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<Page<ConversationSummaryResponse>>> getMyConversations(@RequestParam(defaultValue = "1") int page,
                                                                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Get conversations successfully", chatAIService.getMyConversations(page, size)));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationDetailResponse>> getConversation(@PathVariable String conversationId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Get conversation successfully", chatAIService.getConversation(conversationId)));
    }
}
