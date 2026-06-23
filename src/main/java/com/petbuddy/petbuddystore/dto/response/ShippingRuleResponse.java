package com.petbuddy.petbuddystore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ShippingRuleResponse {
    Long id;
    Double minDistance;
    Double maxDistance;
    BigDecimal fee;
}
