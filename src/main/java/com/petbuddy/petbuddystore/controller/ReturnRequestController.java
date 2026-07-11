package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.CalculateRefundRequest;
import com.petbuddy.petbuddystore.dto.request.CreateReturnRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateReturnStatusRequest;
import com.petbuddy.petbuddystore.dto.response.CalculateRefundResponse;
import com.petbuddy.petbuddystore.dto.response.ReturnRequestResponse;
import com.petbuddy.petbuddystore.service.ReturnRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Return Request API", description = "Quản lý yêu cầu hoàn trả")
public class ReturnRequestController {

    ReturnRequestService returnRequestService;

    // User endpoints
    @PostMapping("/api/returns/calculate-refund")
    public ResponseEntity<ApiResponse<CalculateRefundResponse>> calculateRefund(
            @RequestBody @Valid CalculateRefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Refund calculated successfully",
                returnRequestService.calculateRefund(request)
        ));
    }

    @PostMapping("/api/returns")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> createReturnRequest(
            @RequestBody @Valid CreateReturnRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return request created successfully",
                returnRequestService.createReturnRequest(request)
        ));
    }

    @PostMapping(value = "/api/returns/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> uploadReturnMedia(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(
                "Media uploaded successfully",
                returnRequestService.uploadMedia(id, files)
        ));
    }


    @GetMapping("/api/returns/my")
    public ResponseEntity<ApiResponse<Page<ReturnRequestResponse>>> getMyReturnRequests(
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "My return requests retrieved successfully",
                returnRequestService.getMyReturnRequests(pageable)
        ));
    }

    @GetMapping("/api/returns/{id}")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> getReturnRequestById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return request retrieved successfully",
                returnRequestService.getReturnRequestById(id)
        ));
    }

    @PatchMapping("/api/returns/{id}/cancel")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> cancelReturnRequest(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return request cancelled successfully",
                returnRequestService.cancelReturnRequest(id)
        ));
    }

    // Management endpoints
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @GetMapping("/api/management/returns")
    public ResponseEntity<ApiResponse<Page<ReturnRequestResponse>>> getAllReturnRequests(
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "All return requests retrieved successfully",
                returnRequestService.getAllReturnRequests(pageable)
        ));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @GetMapping("/api/management/returns/{id}")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> getManagementReturnRequestById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return request retrieved successfully",
                returnRequestService.getReturnRequestById(id)
        ));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @PatchMapping("/api/management/returns/{id}/status")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> updateReturnStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateReturnStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Return request status updated successfully",
                returnRequestService.updateReturnStatus(id, request)
        ));
    }
}
