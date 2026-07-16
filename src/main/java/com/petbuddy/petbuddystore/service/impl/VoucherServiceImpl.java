package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.ApplyScope;
import com.petbuddy.petbuddystore.common.enums.DiscountType;
import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import com.petbuddy.petbuddystore.common.enums.VoucherStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.VoucherRequest;
import com.petbuddy.petbuddystore.dto.response.VoucherResponse;
import com.petbuddy.petbuddystore.mapper.VoucherMapper;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.model.UserVouchers;
import com.petbuddy.petbuddystore.model.Voucher;
import com.petbuddy.petbuddystore.repository.PromotionDetailRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.repository.UserVoucherRepository;
import com.petbuddy.petbuddystore.repository.VoucherRepository;
import com.petbuddy.petbuddystore.service.AuditService;
import com.petbuddy.petbuddystore.service.VoucherService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherServiceImpl implements VoucherService {

    VoucherRepository voucherRepository;
    UserRepository userRepository;
    UserVoucherRepository userVoucherRepository;
    PromotionDetailRepository promotionDetailRepository;
    VoucherMapper voucherMapper;

    AuditService auditService;

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
        validateShippingScopeDiscountType(request.getApplyScope(), request.getDiscountType());

        Voucher voucher = voucherMapper.toVoucher(request);
        voucher.setUsedCount(0);
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setUpdatedAt(LocalDateTime.now());

        Voucher saved = voucherRepository.save(voucher);
        auditService.logVoucherCreate(saved, null, null, getCurrentUser());

        return voucherMapper.toVoucherResponse(saved);
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
        validateShippingScopeDiscountType(request.getApplyScope(), request.getDiscountType());

        voucherMapper.updateVoucherFromRequest(request, voucher);
        voucher.setUpdatedAt(LocalDateTime.now());

        return voucherMapper.toVoucherResponse(voucherRepository.save(voucher));
    }
    public Page<VoucherResponse> getActiveVouchers(Pageable pageable) {
        User currentUser = getCurrentUser();
        return voucherRepository.findByStatus(VoucherStatus.ACTIVE, pageable)
                .map(voucher -> {
                    VoucherResponse response = voucherMapper.toVoucherResponse(voucher);
                    if (currentUser != null) {
                        long usedCount = userVoucherRepository.countByUserAndVoucher(currentUser, voucher);
                        response.setUsedByCurrentUser((int) usedCount);
                    }
                    return response;
                });
    }

    @Override
    public BigDecimal applyVoucherToOrder(Order order, String voucherCode, User user, BigDecimal totalAmount) {
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            order.setVoucher(null);
            order.setShippingDiscountAmount(BigDecimal.ZERO);
            return BigDecimal.ZERO;
        }
        validateNoActivePromotionProduct(order);
        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        validateVoucher(voucher, user, totalAmount);

        if (voucher.getApplyScope() == ApplyScope.SHIPPING) {
            BigDecimal shippingDiscount = calculateDiscount(voucher, order.getShippingFee());

            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);

            UserVouchers userVoucher = UserVouchers.builder()
                    .user(user)
                    .voucher(voucher)
                    .usedAt(LocalDateTime.now())
                    .build();
            UserVouchers savedUserVoucher = userVoucherRepository.save(userVoucher);

            order.setVoucher(voucher);
            order.setShippingDiscountAmount(shippingDiscount);
            auditService.logVoucherUsage(savedUserVoucher, user);

            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount = calculateDiscount(voucher, totalAmount);

        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);

        UserVouchers userVoucher = UserVouchers.builder()
                .user(user)
                .voucher(voucher)
                .usedAt(LocalDateTime.now())
                .build();
        UserVouchers savedUserVoucher = userVoucherRepository.save(userVoucher);

        order.setVoucher(voucher);
        order.setShippingDiscountAmount(BigDecimal.ZERO);
        auditService.logVoucherUsage(savedUserVoucher, user);

        return discountAmount;
    }

    @Override
    public void releaseVoucherFromOrder(Order order) {
        Voucher oldVoucher = order.getVoucher();
        if (oldVoucher == null) return;
        oldVoucher.setUsedCount(Math.max(0, oldVoucher.getUsedCount() - 1));
        voucherRepository.save(oldVoucher);
        userVoucherRepository.deleteByUserAndVoucher(order.getUser(), oldVoucher);
        order.setVoucher(null);
        order.setShippingDiscountAmount(BigDecimal.ZERO);
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal baseAmount) {
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = baseAmount.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100));
            if (voucher.getMaxDiscount() != null) {
                discount = discount.min(voucher.getMaxDiscount());
            }
        } else {
            discount = voucher.getDiscountValue();
        }
        return discount.min(baseAmount);
    }

    private void validateVoucher(Voucher voucher, User user, BigDecimal totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStatus() != VoucherStatus.ACTIVE)
            throw new AppException(ErrorCode.VOUCHER_INVALID_STATUS);
        if (now.isBefore(voucher.getStartAt()))
            throw new AppException(ErrorCode.VOUCHER_NOT_STARTED);
        if (now.isAfter(voucher.getExpiredAt()))
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit())
            throw new AppException(ErrorCode.VOUCHER_OUT_OF_USAGE);
        if (voucher.getMinOrderValue() != null && totalAmount.compareTo(voucher.getMinOrderValue()) < 0)
            throw new AppException(ErrorCode.VOUCHER_MIN_ORDER_NOT_MET);
        long userUsedCount = userVoucherRepository.countByUserAndVoucher(user, voucher);
        if (voucher.getPerUserLimit() != null && userUsedCount >= voucher.getPerUserLimit())
            throw new AppException(ErrorCode.VOUCHER_USER_LIMIT_EXCEEDED);
    }

    private void validateShippingScopeDiscountType(ApplyScope applyScope, DiscountType discountType) {
        if (applyScope == ApplyScope.SHIPPING && discountType == DiscountType.PERCENTAGE) {
            throw new AppException(ErrorCode.VOUCHER_SHIPPING_MUST_BE_FIXED_AMOUNT);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new AppException(ErrorCode.UNAUTHENTICATED);
        String userId = authentication.getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
    private void validateNoActivePromotionProduct(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return;
        }
        boolean hasPromotionProduct = order.getOrderDetails().stream()
                .anyMatch(detail -> promotionDetailRepository.existsActivePromotionForProduct(
                        detail.getProduct().getProductId(), PromotionStatus.ACTIVE, null));

        if (hasPromotionProduct) {
            throw new AppException(ErrorCode.VOUCHER_NOT_APPLICABLE_WITH_PROMOTION);
        }
    }
}