package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.ShippingRuleRequest;
import com.petbuddy.petbuddystore.dto.response.ShippingFeeResponse;
import com.petbuddy.petbuddystore.dto.response.ShippingRuleResponse;
import com.petbuddy.petbuddystore.model.ShippingRule;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ShippingMapper {
    ShippingFeeResponse toResponse(Double distanceKm, BigDecimal shippingFee, boolean freeShipping);
    ShippingRuleResponse toShippingResponse(ShippingRule shippingRule);
    ShippingRule toShipping(ShippingRuleRequest request);
}
