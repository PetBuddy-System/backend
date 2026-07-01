package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.model.Category;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRowRequest {
    private int rowNumber;
    private String name;
    private String description;
    private BigDecimal price;
    private String brandName;
    private Category category;
    private Integer stockQuantity;
    private LocalDate expiryDate;
    private String ingredients;
    private String usageInstructions;
    private BigDecimal cost;
    private List<byte[]> images;
}