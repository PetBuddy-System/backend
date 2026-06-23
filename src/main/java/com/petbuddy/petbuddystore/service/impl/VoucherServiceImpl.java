package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.VoucherStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.VoucherRequest;
import com.petbuddy.petbuddystore.dto.response.VoucherResponse;
import com.petbuddy.petbuddystore.mapper.VoucherMapper;
import com.petbuddy.petbuddystore.model.Voucher;
import com.petbuddy.petbuddystore.repository.VoucherRepository;
import com.petbuddy.petbuddystore.service.VoucherService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherMapper voucherMapper;


    public VoucherResponse createVoucher(VoucherRequest request) {
        if (request.getVoucherCode() != null) {
            request.setVoucherCode(request.getVoucherCode().trim().toUpperCase());
        }

        if (voucherRepository.existsByVoucherCode(request.getVoucherCode())) {
            throw new AppException(ErrorCode.VOUCHER_CODE_EXISTED);
        }
        if (request.getExpiredAt().isBefore(request.getStartAt())) {
            throw new AppException(ErrorCode.VOUCHER_INVALID_DATE);
        }

        Voucher voucher = voucherMapper.toVoucher(request);
        voucher.setUsedCount(0);
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setUpdatedAt(LocalDateTime.now());

        return voucherMapper.toVoucherResponse(voucherRepository.save(voucher));
    }

    public VoucherResponse getVoucherById(UUID id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        return voucherMapper.toVoucherResponse(voucher);
    }

    public Page<VoucherResponse> getAllVouchers(Pageable pageable) {
        return voucherRepository.findAll(pageable).map(voucherMapper::toVoucherResponse);
    }

    public VoucherResponse updateVoucher(UUID id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        if (request.getVoucherCode() != null) {
            request.setVoucherCode(request.getVoucherCode().trim().toUpperCase());
        }

        if (voucherRepository.existsByVoucherCodeAndVoucherIdNot(request.getVoucherCode(), id)) {
            throw new AppException(ErrorCode.VOUCHER_CODE_EXISTED);
        }

        voucherMapper.updateVoucherFromRequest(request, voucher);
        voucher.setUpdatedAt(LocalDateTime.now());

        return voucherMapper.toVoucherResponse(voucherRepository.save(voucher));
    }
    public Page<VoucherResponse> getActiveVouchers(Pageable pageable) {
        return voucherRepository.findByStatus(VoucherStatus.ACTIVE, pageable).map(voucherMapper::toVoucherResponse);
    }
}
