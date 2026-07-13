package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.TimeSlotCreationRequest;
import com.petbuddy.petbuddystore.dto.request.TimeSlotUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.TimeSlotResponse;
import com.petbuddy.petbuddystore.service.CatalogTimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog-time-slots")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Catalog Time Slot API", description = "Quan ly khung gio dat lich mau cho dich vu")
public class CatalogTimeSlotController {
    CatalogTimeSlotService catalogTimeSlotService;

    @PostMapping
    @Operation(summary = "Create catalog time slot")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> createTimeSlot(
            @RequestBody @Valid TimeSlotCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Catalog time slot created successfully",
                        catalogTimeSlotService.createTimeSlot(request)));
    }

    @GetMapping("/{timeSlotId}")
    @Operation(summary = "Get catalog time slot by id")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> getTimeSlotById(@PathVariable Integer timeSlotId) {
        return ResponseEntity.ok(ApiResponse.success(catalogTimeSlotService.getTimeSlotById(timeSlotId)));
    }

    @GetMapping("/catalogs/{catalogId}")
    @Operation(summary = "Get all time slots of a catalog")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getTimeSlotsByCatalog(
            @PathVariable Integer catalogId) {
        return ResponseEntity.ok(ApiResponse.success(catalogTimeSlotService.getTimeSlotsByCatalog(catalogId)));
    }

    @GetMapping("/catalogs/{catalogId}/day")
    @Operation(summary = "Get all time slots of a catalog by day of week")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getTimeSlotsByCatalogAndDayOfWeek(
            @PathVariable Integer catalogId,
            @RequestParam DayOfWeek dayOfWeek) {
        return ResponseEntity.ok(ApiResponse.success(
                catalogTimeSlotService.getTimeSlotsByCatalogAndDayOfWeek(catalogId, dayOfWeek)));
    }

    @GetMapping("/catalogs/{catalogId}/available")
    @Operation(summary = "Get active time slots by selected date")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getAvailableTimeSlotsBySelectedDate(
            @PathVariable Integer catalogId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate) {
        return ResponseEntity.ok(ApiResponse.success(
                catalogTimeSlotService.getAvailableTimeSlotsBySelectedDate(catalogId, selectedDate)));
    }

    @PutMapping("/{timeSlotId}")
    @Operation(summary = "Update catalog time slot")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> updateTimeSlot(
            @PathVariable Integer timeSlotId,
            @RequestBody @Valid TimeSlotUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Catalog time slot updated successfully",
                catalogTimeSlotService.updateTimeSlot(timeSlotId, request)));
    }

    @PatchMapping("/{timeSlotId}/active")
    @Operation(summary = "Enable or disable catalog time slot")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> updateActiveStatus(
            @PathVariable Integer timeSlotId,
            @RequestParam @NotNull Boolean isActive) {
        return ResponseEntity.ok(ApiResponse.success("Catalog time slot status updated successfully",
                catalogTimeSlotService.updateActiveStatus(timeSlotId, isActive)));
    }

    @PutMapping("/{timeSlotId}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle catalog time slot active status")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> toggleTimeSlotActive(
            @PathVariable Integer timeSlotId) {
        return ResponseEntity.ok(ApiResponse.success("Catalog time slot active status toggled successfully",
                catalogTimeSlotService.toggleTimeSlotActive(timeSlotId)));
    }
}
