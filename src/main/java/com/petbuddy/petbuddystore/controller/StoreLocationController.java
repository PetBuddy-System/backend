package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.StoreLocationRequest;
import com.petbuddy.petbuddystore.dto.response.StoreLocationResponse;
import com.petbuddy.petbuddystore.service.StoreLocationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/store-locations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Store Location API", description = "Quản lý vị trí cửa hàng")
public class StoreLocationController {
    StoreLocationService storeLocationService;

    @PostMapping
    public ResponseEntity<ApiResponse<StoreLocationResponse>> create(@Valid @RequestBody StoreLocationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Store location created successfully",
                storeLocationService.createLocation(request)));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<StoreLocationResponse>> getCurrent() {
        return ResponseEntity.ok(ApiResponse.success("Current store location retrieved successfully",
                storeLocationService.getCurrentLocation()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreLocationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All store locations retrieved successfully",
                storeLocationService.getAllLocations()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreLocationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Store location retrieved successfully",
                storeLocationService.getLocationById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreLocationResponse>> update(@PathVariable Long id,
                                                     @Valid @RequestBody StoreLocationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Store location updated successfully",
                storeLocationService.updateLocation(id, request)));
    }
}
