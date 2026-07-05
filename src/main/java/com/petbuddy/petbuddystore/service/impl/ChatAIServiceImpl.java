package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.dto.request.ChatAIRequest;
import com.petbuddy.petbuddystore.service.ChatAIService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatAIServiceImpl implements ChatAIService {

    @Override
    public String chat(ChatAIRequest request) {
        return request.getMessage();
    }
}
