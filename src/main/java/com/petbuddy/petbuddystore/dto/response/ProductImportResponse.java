package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImportResponse {
    private boolean success;
    private int createdProducts;
    private int createdBatches;
    private List<Error> errors;

    public record Error(int row, String errorKey) {}
}