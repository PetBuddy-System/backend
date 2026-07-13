package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.ProductUnit;
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
    @Size(max = 255, message = "PRODUCT_NAME_INVALID")
    String name;

    String description;

    String ingredients;

    String usageInstructions;

    @NotNull(message = "PRODUCT_PRICE_REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "PRODUCT_PRICE_INVALID")
    private BigDecimal salePrice;

    @NotNull(message = "PRODUCT_UNIT_REQUIRED")
    private ProductUnit unit;

    @NotBlank(message = "PRODUCT_BRAND_REQUIRED")
    @Size(max = 255, message = "PRODUCT_BRAND_INVALID")
    String brandName;

    @NotNull(message = "CATEGORY_ID_REQUIRED")
    Long categoryId;
}