package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.AuditLogResponse;
import com.petbuddy.petbuddystore.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Log", description = "API quản lý lịch sử thay đổi dữ liệu")
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy danh sách audit logs",
            description = "Lấy danh sách lịch sử thay đổi với các bộ lọc (entityType, entityId, action, date range...)")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @Parameter(description = "Loại entity (PRODUCT, PROMOTION, BATCH, ORDER, USER...)")
            @RequestParam(required = false) AuditEntityType entityType,

            @Parameter(description = "ID của entity")
            @RequestParam(required = false) UUID entityId,

            @Parameter(description = "Hành động (UPDATE, CREATE, DELETE...)")
            @RequestParam(required = false) String action,

            @Parameter(description = "Từ ngày (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @Parameter(description = "Đến ngày (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @Parameter(description = "Người thực hiện (email)")
            @RequestParam(required = false) String performedBy,

            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi trên 1 trang")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sắp xếp: performedAt_desc, performedAt_asc, entityType_desc, entityType_asc")
            @RequestParam(defaultValue = "performedAt_desc") String sortBy
    ) {
        // ⭐ TẠO PAGEABLE KHÔNG SORT để test
        Pageable pageable = PageRequest.of(page, size);  // ⭐ Không có sort

        List<AuditLogResponse> responses = auditService.filterAuditLogs(
                entityType,
                entityId,
                action,
                fromDate,
                toDate,
                performedBy,
                pageable
        );

        Page<AuditLogResponse> responsePage = new PageImpl<>(responses, pageable, responses.size());

        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách audit logs thành công",
                responsePage
        ));
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy lịch sử thay đổi của sản phẩm", description = "Lấy tất cả audit logs của một sản phẩm cụ thể")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByProduct(
            @Parameter(description = "ID của sản phẩm")
            @PathVariable UUID productId
    ) {
        // ⭐ Service trả về List<AuditLogResponse> rồi
        List<AuditLogResponse> responses = auditService.getAuditLogsByEntity(
                AuditEntityType.PRODUCT,
                productId
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thay đổi của sản phẩm thành công", responses));
    }

    @GetMapping("/promotions/{promotionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy lịch sử thay đổi của chương trình khuyến mãi", description = "Lấy tất cả audit logs của một chương trình khuyến mãi cụ thể")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByPromotion(
            @Parameter(description = "ID của chương trình khuyến mãi")
            @PathVariable UUID promotionId
    ) {
        List<AuditLogResponse> responses = auditService.getAuditLogsByEntity(AuditEntityType.PROMOTION, promotionId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thay đổi của chương trình khuyến mãi thành công", responses));
    }


    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lấy lịch sử thay đổi của lô hàng", description = "Lấy tất cả audit logs của một lô hàng cụ thể")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogsByBatch(
            @Parameter(description = "ID của lô hàng")
            @PathVariable UUID batchId
    ) {
        List<AuditLogResponse> responses = auditService.getAuditLogsByEntity(AuditEntityType.BATCH, batchId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thay đổi của lô hàng thành công", responses));
    }

    @GetMapping("/{auditLogId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy chi tiết audit log", description = "Lấy thông tin chi tiết của một audit log cụ thể")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(
            @Parameter(description = "ID của audit log")
            @PathVariable UUID auditLogId
    ) {
        AuditLogResponse response = auditService.getAuditLogById(auditLogId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết audit log thành công", response));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy audit logs gần đây", description = "Lấy danh sách audit logs gần đây nhất (mặc định 10 bản ghi)")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentAuditLogs(
            @Parameter(description = "Số lượng bản ghi (mặc định 10)")
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<AuditLogResponse> responses = auditService.getRecentAuditLogs(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy audit logs gần đây thành công", responses));
    }
}