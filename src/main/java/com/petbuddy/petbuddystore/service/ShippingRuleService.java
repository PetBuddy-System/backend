package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.ShippingRuleRequest;
import com.petbuddy.petbuddystore.dto.response.ShippingFeeResponse;
import com.petbuddy.petbuddystore.dto.response.ShippingRuleResponse;
import com.petbuddy.petbuddystore.model.ShippingRule;

import java.util.List;

public interface ShippingRuleService {
    ShippingFeeResponse calculateFee(Double latitude, Double longitude);
    ShippingRuleResponse createRule(ShippingRuleRequest request);
    ShippingRuleResponse updateRule(Long id, ShippingRuleRequest request);
    List<ShippingRule> getAllShippingRules();
    ShippingRule getShippingRuleById(Long id);
    void deleteShippingRule(Long id);

}
