package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.response.DeliveryStopResponse;
import com.petbuddy.petbuddystore.dto.response.ShipperSuggestionResponse;
import com.petbuddy.petbuddystore.service.ShipperAssignmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipper-assignment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Shipper Assignment", description = "Endpoints for shipper assignment and delivery route suggestions")
public class ShipperAssignmentController {

    ShipperAssignmentService shipperAssignmentService;

    @GetMapping("/{orderId}/shipper-suggestions")
    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<ShipperSuggestionResponse>>> getShipperSuggestions(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Get shipper suggestion successful", shipperAssignmentService.getShipperSuggestions(orderId)));
    }

    @PostMapping("/{orderId}/assign-shipper")
    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_COORDINATOR')")
    public ResponseEntity<ApiResponse<Void>> assignShipper(@PathVariable Long orderId, @RequestParam String staffId) {
        shipperAssignmentService.assignShipper(orderId, staffId);
        return ResponseEntity.ok(ApiResponse.success("Assign shipper successful"));
    }

    @GetMapping("/{staffId}/delivery-route")
    @PreAuthorize("hasRole('STAFF') and hasAuthority('TASK_SHIPPER')")
    public ResponseEntity<ApiResponse<List<DeliveryStopResponse>>> suggestDeliveryRoute(@RequestParam String staffId) {
        return ResponseEntity.ok(ApiResponse.success("Suggest delivery route successful", shipperAssignmentService.suggestDeliveryRoute(staffId)));
    }
}