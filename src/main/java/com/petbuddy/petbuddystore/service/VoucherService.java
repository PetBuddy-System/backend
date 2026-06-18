package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.VoucherRequest;
import com.petbuddy.petbuddystore.dto.response.VoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VoucherService {
    VoucherResponse createVoucher(VoucherRequest request);

    VoucherResponse updateVoucher(UUID voucherId, VoucherRequest request);

    Page<VoucherResponse> getAllVouchers(Pageable pageable);

    VoucherResponse getVoucherById(UUID id);
}
