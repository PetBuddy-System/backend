package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.VoucherRequest;
import com.petbuddy.petbuddystore.dto.response.VoucherResponse;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface VoucherService {
    VoucherResponse createVoucher(VoucherRequest request);
    VoucherResponse updateVoucher(UUID voucherId, VoucherRequest request);
    Page<VoucherResponse> getAllVouchers(Pageable pageable);
    VoucherResponse getVoucherById(UUID id);
    Page<VoucherResponse> getActiveVouchers(Pageable pageable);
    BigDecimal applyVoucherToOrder(Order order, String voucherCode, User user, BigDecimal totalAmount);
    void releaseVoucherFromOrder(Order order);
}
