package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.response.DeliveryStopResponse;
import com.petbuddy.petbuddystore.dto.response.RestockEligibilityResponse;
import com.petbuddy.petbuddystore.dto.response.ShipperSuggestionResponse;

import java.util.List;

public interface ShipperAssignmentService {
    void assignShipper(Long orderId, String staffId);
    List<ShipperSuggestionResponse> getShipperSuggestions(Long orderId);
    List<DeliveryStopResponse> suggestDeliveryRoute(String staffId);
    void recomputeDailyZones();
    List<RestockEligibilityResponse> getShippersEligibleForRestock();
    void updateEstimatedDeliveryTime(Long orderId);
}
