package com.petbuddy.petbuddystore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.petbuddy.petbuddystore.common.enums.ChatInputType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatAIResponse {
    ChatInputType chatInputType;
//    String conversationId;
//    String transcript;
    String answer;
//    RiskLevel riskLevel;
//    List<String> possibleCauses;
//    List<String> warningSigns;
    List<String> suggestions;
    List<String> followUpQuestions;
    String disclaimer;
}
