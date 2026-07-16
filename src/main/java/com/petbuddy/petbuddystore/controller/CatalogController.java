package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.CatalogStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.CatalogCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CatalogUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CatalogResponse;
import com.petbuddy.petbuddystore.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalogs")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Catalog API", description = "Quản lý dịch vụ chăm sóc")
public class CatalogController {
    CatalogService catalogService;


    @PostMapping("/create")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Create pet service catalog",
            description = "Tạo mới một dịch vụ chăm sóc thú cưng."
    )
    public ResponseEntity<ApiResponse<CatalogResponse>> createCatalog(
            @Valid @RequestBody CatalogCreationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Catalog created successfully",
                        catalogService.createCatalog(request)
                ));
    }

    @GetMapping
    @Operation(
            summary = "Get all pet service catalogs",
            description = "Lấy danh sách tất cả các dịch vụ chăm sóc thú cưng."
    )
    public ResponseEntity<ApiResponse<List<CatalogResponse>>> getAllCatalogs() {
        return ResponseEntity.ok(
                ApiResponse.success(catalogService.getAllCatalogs())
        );
    }

    @GetMapping("/{catalogId}")
    @Operation(
            summary = "Get catalog by id",
            description = "Lấy chi tiết dịch vụ chăm sóc theo catalogId."
    )
    public ResponseEntity<ApiResponse<CatalogResponse>> getCatalogById(
            @PathVariable int catalogId) {
        return ResponseEntity.ok(
                ApiResponse.success(catalogService.getCatalogById(catalogId))
        );
    }
    @PutMapping("/{catalogId}/update")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Update catalog",
            description = "Cập nhật dịch vụ chăm sóc thú cưng (hỗ trợ cập nhật lẻ các trường)."
    )
    public ResponseEntity<ApiResponse<CatalogResponse>> updateCatalog(
            @PathVariable int catalogId,
            @Valid @RequestBody CatalogUpdateRequest updateRequest) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Catalog updated successfully",
                        catalogService.updateCatalog(catalogId, updateRequest)
                )
        );
    }

    @PutMapping("/{catalogId}/status")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Update catalog status")
    public ResponseEntity<ApiResponse<CatalogResponse>> updateCatalogStatus(
            @PathVariable Integer catalogId,
            @RequestParam CatalogStatus status) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Catalog status updated successfully",
                        catalogService.updateCatalogStatus(catalogId, status)
                )
        );
    }
}
