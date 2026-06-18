package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.VoucherRequest;
import com.petbuddy.petbuddystore.dto.response.VoucherResponse;
import com.petbuddy.petbuddystore.service.VoucherService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
@Tag(name = "Voucher API", description = "Quản lý voucher giảm giá")
public class VoucherController {
    private final VoucherService voucherService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> create(@RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo voucher thành công",
                voucherService.createVoucher(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy voucher thành công",
                voucherService.getVoucherById(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách voucher thành công",
                voucherService.getAllVouchers(pageable)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VoucherResponse>> update(@PathVariable UUID id, @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật voucher thành công",
                voucherService.updateVoucher(id, request)));
    }
}
