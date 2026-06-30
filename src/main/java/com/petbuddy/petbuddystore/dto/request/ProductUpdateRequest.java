package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
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
    @Size(max = 255)
    String name;

    @Size(max = 2000)
    String description;

    @Size(max = 1000)
    String ingredients;

    @Size(max = 1000)
    String usageInstructions;

    @DecimalMin(value = "0.01")
    BigDecimal price;

    String brandName;

    Long categoryId;

    ProductStatus status;

    Boolean mergeIfNameExists;
}