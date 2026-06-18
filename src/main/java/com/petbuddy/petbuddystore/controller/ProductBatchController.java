package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.ProductBatchCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductBatchUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBatchResponse;
import com.petbuddy.petbuddystore.dto.response.ProductImportResponse;
import com.petbuddy.petbuddystore.service.ProductBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Product Batch API", description = "Quản lý lô hàng của sản phẩm")
public class ProductBatchController {

    ProductBatchService productBatchService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/api/products/{productId}/batches")
    @Operation(summary = "Create product batches",
            description = "Tạo một hoặc nhiều lô hàng cho sản phẩm. Giới hạn tối đa 10 batch/lần tạo. BatchCode được backend tự sinh sau khi xác nhận."
    )
    public ResponseEntity<ApiResponse<List<ProductBatchResponse>>> createBatches(
            @PathVariable UUID productId,
            @Valid @RequestBody List<ProductBatchCreationRequest> requests
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product batches created successfully", productBatchService.createBatches(productId, requests)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @GetMapping("/api/products/{productId}/batches")
    @Operation(summary = "Get product batches",
            description = "Lấy danh sách batch của một sản phẩm, có hỗ trợ phân trang, tìm kiếm theo batchCode, lọc status và sort. Mặc định sort theo createdAt mới nhất để batch vừa tạo nằm đầu danh sách."
    )
    public ResponseEntity<ApiResponse<Page<ProductBatchResponse>>> getBatchesByProduct(
            @PathVariable UUID productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "date_desc") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(productBatchService.getBatchesByProduct(productId, keyword, status, sortBy, pageable)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @GetMapping("/api/batches/{batchId}")
    @Operation(summary = "Get batch detail",
            description = "Lấy chi tiết một batch theo batchId để hiển thị hoặc chỉnh sửa."
    )
    public ResponseEntity<ApiResponse<ProductBatchResponse>> getBatchDetail(@PathVariable UUID batchId) {
        return ResponseEntity.ok(ApiResponse.success(productBatchService.getBatchDetail(batchId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/api/batches/{batchId}")
    @Operation(summary = "Update product batch",
            description = "Cập nhật stock, expiryDate hoặc status của batch. Khi status = DELETED, batch được xóa mềm và có thể bị xóa vĩnh viễn sau 90 ngày."
    )
    public ResponseEntity<ApiResponse<ProductBatchResponse>> updateBatch(@PathVariable UUID batchId, @Valid @RequestBody ProductBatchUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product batch updated successfully", productBatchService.updateBatch(batchId, request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping(value = "/api/products/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import products and batches from Excel",
            description = "Import Excel theo cơ chế preview/confirm. confirm=false chỉ kiểm tra và trả warnings/errors, confirm=true mới tạo Product và ProductBatch thật."
    )
    public ResponseEntity<ApiResponse<ProductImportResponse>> importProductsAndBatches(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean confirm
    ) {
        return ResponseEntity.ok(ApiResponse.success(confirm ? "Import completed successfully" : "Import preview completed", productBatchService.importProductsAndBatches(file, confirm)));
    }
}