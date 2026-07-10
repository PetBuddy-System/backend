package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatchCreationListRequest {
    
    @NotEmpty(message = "BATCH_REQUIRED")
    @Size(max = 10, message = "BATCH_LIMIT_EXCEEDED")
    @Valid
    private List<ProductBatchCreationRequest> batches;
}