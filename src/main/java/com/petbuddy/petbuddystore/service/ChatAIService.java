package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.ChatAIRequest;

public interface ChatAIService {
    String chat(ChatAIRequest request);
}
