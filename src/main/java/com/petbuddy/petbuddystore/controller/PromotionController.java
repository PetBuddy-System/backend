package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.PromotionRequest;
import com.petbuddy.petbuddystore.dto.request.PromotionUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.PromotionListResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionResponse;
import com.petbuddy.petbuddystore.service.PromotionService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotion API", description = "Quản lý chương trình khuyến mãi")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionController {

    PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Tạo chương trình khuyến mãi", description = "Tạo mới chương trình khuyến mãi và danh sách sản phẩm được áp dụng.")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Tạo chương trình khuyến mãi thành công",
                        promotionService.createPromotion(request)
                ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Danh sách tất cả promotion", description = "Lấy danh sách tất cả promotion có phân trang, filter theo status và tìm kiếm theo tên hoặc mô tả.")
    public ResponseEntity<ApiResponse<Page<PromotionListResponse>>> getPromotions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt_desc") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách chương trình khuyến mãi thành công",
                promotionService.getPromotions(keyword, status, pageable, sortBy)
        ));
    }

    @GetMapping("/{promotionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Chi tiết promotion", description = "Lấy chi tiết thông tin chương trình khuyến mãi bao gồm cả danh sách sản phẩm và mức giảm giá.")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionById(@PathVariable UUID promotionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin chi tiết chương trình khuyến mãi thành công",
                promotionService.getPromotionById(promotionId)
        ));
    }

    @PatchMapping("/{promotionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Cập nhật promotion", description = "Cập nhật thông tin chương trình khuyến mãi (thời gian, trạng thái, danh sách sản phẩm, mức giảm giá...).")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(
            @PathVariable UUID promotionId,
            @Valid @RequestBody PromotionUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật chương trình khuyến mãi thành công",
                promotionService.updatePromotion(promotionId, request)
        ));
    }
}
