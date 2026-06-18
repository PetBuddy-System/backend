package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCreationRequest {
    @NotBlank(message = "PRODUCT_NAME_REQUIRED")
    String name;

    String description;

    @NotNull(message = "PRODUCT_PRICE_REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "PRODUCT_PRICE_INVALID")
    BigDecimal price;

    String brandName;

    @NotNull(message = "CATEGORY_REQUIRED")
    Long categoryId;
}