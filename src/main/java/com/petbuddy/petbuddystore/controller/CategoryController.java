package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.CategoryCreationRequest;
import com.petbuddy.petbuddystore.dto.request.CategoryUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.CategoryResponse;
import com.petbuddy.petbuddystore.service.CategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Category API", description = "Quản lý danh mục sản phẩm")
public class CategoryController {

    CategoryService categoryService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @PostMapping
    @Operation(summary = "Create categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> createCategories(
            @Valid @RequestBody List<CategoryCreationRequest> requests
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Categories created successfully",
                        categoryService.createCategories(requests)
                ));
    }

    @GetMapping
    @Operation(summary = "Get active categories for user UI")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getActiveCategories()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @GetMapping("/management")
    @Operation(summary = "Get all categories for management")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategoriesForManagement() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategoriesForManagement()));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category by id")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(categoryId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @PatchMapping("/{categoryId}")
    @Operation(summary = "Update category info/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Category updated successfully",
                        categoryService.updateCategory(categoryId, request)
                )
        );
    }
}