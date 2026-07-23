package com.petbuddy.petbuddystore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnStatisticsResponse {

    Long totalRequests;

    Long assigned;

    Long pending;

    Long approved;

    Long completed;

    Long rejected;
}