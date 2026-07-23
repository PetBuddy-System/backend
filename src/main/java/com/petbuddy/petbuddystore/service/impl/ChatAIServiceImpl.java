package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.AIConversationStatus;
import com.petbuddy.petbuddystore.common.enums.AIMessageRole;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.configuration.PromptBuilder;
import com.petbuddy.petbuddystore.dto.request.ChatAIRequest;
import com.petbuddy.petbuddystore.dto.response.ChatAIResponse;
import com.petbuddy.petbuddystore.dto.response.ConversationDetailResponse;
import com.petbuddy.petbuddystore.dto.response.ConversationMessageResponse;
import com.petbuddy.petbuddystore.dto.response.ConversationSummaryResponse;
import com.petbuddy.petbuddystore.mapper.MediaFileMapper;
import com.petbuddy.petbuddystore.model.AIConversation;
import com.petbuddy.petbuddystore.model.AIConversationMessage;
import com.petbuddy.petbuddystore.model.MediaFile;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.AIConversationMessageRepository;
import com.petbuddy.petbuddystore.repository.AIConversationRepository;
import com.petbuddy.petbuddystore.service.ChatAIService;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatAIServiceImpl implements ChatAIService {

    ChatClient chatClient;
    PromptBuilder promptBuilder;
    UserService userService;
    FileService fileService;
    MediaFileMapper mediaFileMapper;
    AIConversationRepository aiConversationRepository;
    AIConversationMessageRepository aiConversationMessageRepository;

    @Override
    public ChatAIResponse chat(ChatAIRequest request, List<MultipartFile> images) {
        validateRequest(request, images);
        User currentUser = userService.getCurrentUserEntityOrNull();

        if (currentUser == null) {
            return guestChat(request, images);
        }
        return authenticatedChat(request, images, currentUser);
    }

    @Override
    public Page<ConversationSummaryResponse> getMyConversations(int page, int size) {
        User currentUser = userService.getCurrentUserEntity();
        Pageable pageable = PageRequest.of(page - 1, size);

        return aiConversationRepository.findByUserUserIdAndAiConversationStatusOrderByUpdatedAtDesc(
                        currentUser.getUserId(), AIConversationStatus.ACTIVE, pageable)

                .map(conversation -> ConversationSummaryResponse.builder()
                        .conversationId(conversation.getConversationId())
                        .title(conversation.getTitle())
                        .createdAt(conversation.getCreatedAt())
                        .updatedAt(conversation.getUpdatedAt())
                        .build());
    }

    @Override
    public ConversationDetailResponse getConversation(String conversationId) {
        User currentUser = userService.getCurrentUserEntity();

        AIConversation conversation = aiConversationRepository.findByConversationIdAndUserUserIdAndAiConversationStatus(conversationId,
                        currentUser.getUserId(), AIConversationStatus.ACTIVE)
                        .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        List<ConversationMessageResponse> messages = aiConversationMessageRepository.findByAiConversationOrderByCreatedAtAsc(conversation)
                        .stream()
                        .map(message -> ConversationMessageResponse.builder()
                                .conversationMessageId(message.getConversationMessageId())
                                .role(message.getMessageRole())
                                .content(message.getContent())
                                .mediaFiles(mediaFileMapper.toMediaFileResponses(message.getMediaFiles()))
                                .createdAt(message.getCreatedAt())
                                .build())
                        .toList();

        return ConversationDetailResponse.builder()
                .conversationId(conversation.getConversationId())
                .title(conversation.getTitle())
                .messages(messages)
                .build();
    }

    private String callAI(List<Message> promptMessages) {
        try {
            String rawAnswer = chatClient.prompt()
                    .messages(promptMessages)
                    .call()
                    .content();
            return removeThinking(rawAnswer);

        } catch (Exception e) {
            log.error("AI chat failed", e);
            throw new AppException(ErrorCode.AI_CHAT_FAILED);
        }
    }

    private ChatAIResponse guestChat(ChatAIRequest request, List<MultipartFile> images) {
        List<Media> aiMediaList = buildAIMedia(images);
        List<Message> promptMessages = promptBuilder.buildMessages(List.of(), request.getMessage(), aiMediaList);

        String answer = callAI(promptMessages);
        return ChatAIResponse.builder()
                .conversationId(null)
                .answer(answer)
                .mediaFiles(List.of())
                .build();
    }

    private ChatAIResponse authenticatedChat(ChatAIRequest request, List<MultipartFile> images, User currentUser) {
        AIConversation aiConversation = getOrCreateConversation(request, currentUser);

        AIConversationMessage userMessage = AIConversationMessage.builder()
                .messageRole(AIMessageRole.USER)
                .content(request.getMessage() == null ? "" : request.getMessage())
                .aiConversation(aiConversation)
                .build();

        List<MediaFile> mediaFiles = uploadAIChatImages(images, userMessage);
        userMessage.setMediaFiles(mediaFiles);

        AIConversationMessage savedUserMessage = aiConversationMessageRepository.save(userMessage);
        List<AIConversationMessage> historyMessages = getHistoryMessages(aiConversation, savedUserMessage.getConversationMessageId());
        List<Message> promptMessages = promptBuilder.buildMessages(historyMessages, request.getMessage(), buildAIMedia(images));

        String answer = callAI(promptMessages);

        AIConversationMessage assistantMessage = AIConversationMessage.builder()
                .messageRole(AIMessageRole.ASSISTANT)
                .content(answer)
                .aiConversation(aiConversation)
                .build();

        aiConversationMessageRepository.save(assistantMessage);

        return ChatAIResponse.builder()
                .conversationId(aiConversation.getConversationId())
                .answer(answer)
                .mediaFiles(mediaFileMapper.toMediaFileResponses(mediaFiles))
                .build();
    }

    private void validateRequest(ChatAIRequest request, List<MultipartFile> images) {
        boolean emptyMessage = request.getMessage() == null || request.getMessage().isBlank();

        boolean emptyImages = images == null || images.isEmpty()
                || images.stream().allMatch(file -> file == null || file.isEmpty());

        if (emptyMessage && emptyImages) {
            throw new AppException(ErrorCode.MESSAGE_OR_IMAGE_REQUIRED);
        }

        if (images != null && images.size() > 3) {
            throw new AppException(ErrorCode.IMAGE_SIZE_INVALID);
        }
    }

    private AIConversation getOrCreateConversation(ChatAIRequest request, User currentUser) {
        if (request.getConversationId() == null || request.getConversationId().isBlank()) {
            AIConversation conversation = AIConversation.builder()
                    .title(buildConversationTitle(request.getMessage()))
                    .aiConversationStatus(AIConversationStatus.ACTIVE)
                    .user(currentUser)
                    .build();

            return aiConversationRepository.save(conversation);
        }

        return aiConversationRepository.findByConversationIdAndUserUserIdAndAiConversationStatus(
                        request.getConversationId(), currentUser.getUserId(), AIConversationStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    private String buildConversationTitle(String message) {
        if (message == null || message.isBlank()) {
            return "Cuộc trò chuyện mới";
        }

        String cleanMessage = message.trim();

        return cleanMessage.length() > 50
                ? cleanMessage.substring(0, 50)
                : cleanMessage;
    }

    private List<AIConversationMessage> getHistoryMessages(AIConversation conversation, String currentMessageId) {
        return aiConversationMessageRepository
                .findTop20ByAiConversationOrderByCreatedAtDesc(conversation)
                .stream()
                .filter(message -> !message.getConversationMessageId().equals(currentMessageId))
                .sorted(Comparator.comparing(AIConversationMessage::getCreatedAt))
                .toList();
    }

    private List<MediaFile> uploadAIChatImages(List<MultipartFile> images, AIConversationMessage userMessage) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .filter(image -> image != null && !image.isEmpty())
                .map(file -> {
                    MediaFile mediaFile = fileService.uploadAIChatImage(file);
                    mediaFile.setAiConversationMessage(userMessage);
                    return mediaFile;
                })
                .collect(Collectors.toList());
    }

    private List<Media> buildAIMedia(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(this::toAIMedia)
                .toList();
    }

    private Media toAIMedia(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") &&
                    !contentType.equals("image/webp") && !contentType.equals("image/jpg"))) {
                throw new AppException(ErrorCode.INVALID_FILE_TYPE);
            }

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            return new Media(MimeTypeUtils.parseMimeType(contentType), resource);

        } catch (IOException e) {
            throw new AppException(ErrorCode.MEDIA_READ_FAILED);
        }
    }

    private String removeThinking(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String cleaned = content;
        cleaned = cleaned.replaceAll("(?is)<think\\b[^>]*>.*?</think>", "");
        cleaned = cleaned.replaceAll("(?is)<think\\b[^>]*>.*$", "");
        cleaned = cleaned.replaceAll("(?i)</?think\\b[^>]*>", "");
        return cleaned.trim();
    }
}