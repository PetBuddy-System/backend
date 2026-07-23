package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.ChatAIRequest;
import com.petbuddy.petbuddystore.dto.response.ChatAIResponse;
import com.petbuddy.petbuddystore.dto.response.ConversationDetailResponse;
import com.petbuddy.petbuddystore.dto.response.ConversationSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatAIService {
    ChatAIResponse chat(ChatAIRequest request, List<MultipartFile> images);
    Page<ConversationSummaryResponse> getMyConversations(int page, int size);
    ConversationDetailResponse getConversation(String conversationId);
}
