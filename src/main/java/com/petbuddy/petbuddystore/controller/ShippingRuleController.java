package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.ShippingRuleRequest;
import com.petbuddy.petbuddystore.dto.response.ShippingFeeResponse;
import com.petbuddy.petbuddystore.dto.response.ShippingRuleResponse;
import com.petbuddy.petbuddystore.model.ShippingRule;
import com.petbuddy.petbuddystore.service.ShippingRuleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-rules")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Shipping Rule API", description = "APIs for calculating shipping fees based on location")
public class ShippingRuleController {

    ShippingRuleService shippingRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingRule>>> getAllShippingRules() {
        return ResponseEntity.ok(ApiResponse.success(
                "Get Shipping Rule Successfully",
                shippingRuleService.getAllShippingRules()));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingRule>> getShippingRuleById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get Shipping Rule Successfully",
                shippingRuleService.getShippingRuleById(Long.parseLong(id))));
    }
    @GetMapping("/fee")
    public ResponseEntity<ApiResponse<ShippingFeeResponse>> getFee(@RequestParam Double latitude, @RequestParam Double longitude){
        return ResponseEntity.ok(ApiResponse.success(shippingRuleService.calculateFee(latitude, longitude)));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<ShippingRuleResponse>> createFee(@RequestBody ShippingRuleRequest request){
        return ResponseEntity.ok(ApiResponse.success(shippingRuleService.createRule(request)));
    }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingRuleResponse>> updateFee(@PathVariable Long id, @RequestBody ShippingRuleRequest request){
        return ResponseEntity.ok(ApiResponse.success(shippingRuleService.updateRule(id, request)));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFee(@PathVariable Long id){
        shippingRuleService.deleteShippingRule(id);
        return ResponseEntity.ok(ApiResponse.success("Delete Shipping Rule Successfully", null));
    }
}
