package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.response.AuditLogResponse;
import com.petbuddy.petbuddystore.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            description = "Lấy danh sách lịch sử thay đổi với các bộ lọc...")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @Parameter(description = "Loại entity (PRODUCT, PROMOTION, BATCH, ORDER, USER...)")
            @RequestParam(required = false) AuditEntityType entityType,

            @Parameter(description = "Code của entity (VD: PRDF7D898, PRMA1B2C3...)")
            @RequestParam(required = false) String entityCode,

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
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "performedAt"));

        Page<AuditLogResponse> responsePage = auditService.filterAuditLogs(
                entityType,
                entityCode,
                action,
                fromDate,
                toDate,
                performedBy,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách audit logs thành công", responsePage));
    }

    @GetMapping("/{auditLogId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy chi tiết audit log",
            description = "Lấy thông tin chi tiết của một audit log cụ thể")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(
            @Parameter(description = "ID của audit log")
            @PathVariable UUID auditLogId
    ) {
        // ⭐ Service trả về DTO
        AuditLogResponse response = auditService.getAuditLogById(auditLogId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết audit log thành công", response));
    }
}