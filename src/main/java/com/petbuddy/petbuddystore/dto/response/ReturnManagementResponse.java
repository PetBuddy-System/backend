package com.petbuddy.petbuddystore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnManagementResponse {

    private Page<ReturnRequestResponse> returns;

    private ReturnStatisticsResponse statistics;
}