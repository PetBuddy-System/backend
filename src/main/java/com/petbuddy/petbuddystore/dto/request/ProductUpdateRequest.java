package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.enums.ProductUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    @Size(max = 255, message = "PRODUCT_NAME_INVALID")
    String name;

    @Size(max = 2000, message = "PRODUCT_DESCRIPTION_INVALID")
    String description;

    @Size(max = 1000, message = "PRODUCT_INGREDIENTS_INVALID")
    String ingredients;

    @Size(max = 1000, message = "PRODUCT_USAGE_INSTRUCTIONS_INVALID")
    String usageInstructions;

    @DecimalMin(value = "0.01", message = "PRODUCT_PRICE_INVALID")
    BigDecimal salePrice;

    ProductUnit unit;

    @Size(max = 255, message = "PRODUCT_BRAND_INVALID")
    String brandName;

    Long categoryId;

    ProductStatus status;

    Long thumbnailMediaId;

    @Size(max = 500, message = "PRODUCT_REASON_INVALID")
    String reason;

    @Size(max = 1000, message = "PRODUCT_NOTE_INVALID")
    String note;

}