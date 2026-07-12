package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.CalculateRefundRequest;
import com.petbuddy.petbuddystore.dto.request.CreateReturnRequest;
import com.petbuddy.petbuddystore.dto.request.ReturnFilterRequest;
import com.petbuddy.petbuddystore.dto.request.ReturnItemRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateReturnStatusRequest;
import com.petbuddy.petbuddystore.dto.response.CalculateRefundResponse;
import com.petbuddy.petbuddystore.dto.response.ReturnItemResponse;
import com.petbuddy.petbuddystore.dto.response.ReturnRequestResponse;
import com.petbuddy.petbuddystore.mapper.ReturnRequestMapper;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.service.FileService;
import com.petbuddy.petbuddystore.service.PaymentService;
import com.petbuddy.petbuddystore.service.ReturnRequestService;
import com.petbuddy.petbuddystore.service.ReturnStockService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReturnRequestServiceImpl implements ReturnRequestService {

    ReturnRequestRepository returnRequestRepository;
    ReturnItemRepository returnItemRepository;
    OrderRepository orderRepository;
    OrderDetailRepository orderDetailRepository;
    UserRepository userRepository;
    FileService fileService;
    PaymentService paymentService;
    ReturnRequestMapper returnRequestMapper;
    ReturnStockService returnStockService;

    @Override
    public CalculateRefundResponse calculateRefund(CalculateRefundRequest request) {
        checkLogin();
        User currentUser = getCurrentUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ORDER_NOT_OWNED);
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        LocalDate completedDate = order.getUpdatedAt().toLocalDate();
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(completedDate, today);
        if (days > 30) {
            throw new AppException(ErrorCode.RETURN_PERIOD_EXPIRED);
        }

        ReturnReason reasonEnum;
        try {
            reasonEnum = ReturnReason.valueOf(request.getReason());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        List<ReturnItemResponse> itemsResponse = new ArrayList<>();

        BigDecimal policyFactor = getRefundPolicyFactor(reasonEnum, days);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        for (ReturnItemRequest itemReq : request.getItems()) {
            OrderDetail orderDetail = orderDetailRepository.findById(itemReq.getOrderDetailId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

            if (!orderDetail.getOrder().getOrderId().equals(order.getOrderId())) {
                throw new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND);
            }

            if (itemReq.getQuantity() <= 0) {
                throw new AppException(ErrorCode.RETURN_QUANTITY_INVALID);
            }

            int alreadyReturned = returnItemRepository.sumQuantityByOrderDetailIdAndStatusIn(
                    orderDetail.getOrderDetailId(),
                    List.of(ReturnStatus.PENDING, ReturnStatus.APPROVED, ReturnStatus.COMPLETED));

            if (itemReq.getQuantity() > orderDetail.getQuantity() - alreadyReturned) {
                throw new AppException(ErrorCode.RETURN_QUANTITY_EXCEEDED);
            }

            BigDecimal orderTotal = order.getTotalAmount();
            BigDecimal discount = order.getDiscountAmount();

            BigDecimal allocatedDetailDiscount = (discount != null && orderTotal.compareTo(BigDecimal.ZERO) > 0)
                    ? discount.multiply(orderDetail.getTotalPrice()).divide(orderTotal, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal detailValueAfterVoucher = orderDetail.getTotalPrice().subtract(allocatedDetailDiscount);
            BigDecimal unitValueAfterVoucher = detailValueAfterVoucher
                    .divide(BigDecimal.valueOf(orderDetail.getQuantity()), 4, RoundingMode.HALF_UP);
            BigDecimal requestedValue = unitValueAfterVoucher.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            BigDecimal itemRefund = requestedValue.multiply(policyFactor).setScale(2, RoundingMode.HALF_UP);
            totalRefundAmount = totalRefundAmount.add(itemRefund);

            itemsResponse.add(ReturnItemResponse.builder()
                    .orderDetailId(orderDetail.getOrderDetailId())
                    .productName(orderDetail.getProductName())
                    .productImage(orderDetail.getProductImage())
                    .quantity(itemReq.getQuantity())
                    .refundAmount(itemRefund)
                    .build());
        }

        return CalculateRefundResponse.builder()
                .totalRefundAmount(totalRefundAmount)
                .items(itemsResponse)
                .build();
    }

    @Override
    public ReturnRequestResponse createReturnRequest(CreateReturnRequest request) {
        checkLogin();
        User currentUser = getCurrentUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ORDER_NOT_OWNED);
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        LocalDate completedDate = order.getUpdatedAt().toLocalDate();
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(completedDate, today);
        if (days > 30) {
            throw new AppException(ErrorCode.RETURN_PERIOD_EXPIRED);
        }

        ReturnType typeEnum;
        ReturnReason reasonEnum;
        RefundMethod refundMethodEnum;
        try {
            typeEnum = ReturnType.valueOf(request.getType());
            reasonEnum = ReturnReason.valueOf(request.getReason());
            refundMethodEnum = RefundMethod.valueOf(request.getRefundMethod());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (refundMethodEnum == RefundMethod.BANK_TRANSFER) {
            if (!org.springframework.util.StringUtils.hasText(request.getBankName())
                    || !org.springframework.util.StringUtils.hasText(request.getBankAccountNumber())
                    || !org.springframework.util.StringUtils.hasText(request.getBankAccountHolder())) {
                throw new AppException(ErrorCode.BANK_INFO_REQUIRED);
            }
        } else if (refundMethodEnum == RefundMethod.STRIPE_PAYMENT) {
            if (request.getBankName() != null
                    || request.getBankAccountNumber() != null
                    || request.getBankAccountHolder() != null) {
                throw new AppException(ErrorCode.BANK_INFO_MUST_BE_NULL);
            }
        }

        if (typeEnum == ReturnType.EXCHANGE) {
            for (ReturnItemRequest itemReq : request.getItems()) {
                OrderDetail orderDetail = orderDetailRepository.findById(itemReq.getOrderDetailId())
                        .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

                ReturnItem tempItem = ReturnItem.builder()
                        .orderDetail(orderDetail)
                        .quantity(itemReq.getQuantity())
                        .build();

                returnStockService.validateStockForExchange(tempItem);
            }
        }

        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        List<ReturnItem> returnItems = new ArrayList<>();

        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnCode(generateReturnCode())
                .order(order)
                .requestedBy(currentUser)
                .type(typeEnum)
                .reason(reasonEnum)
                .description(request.getDescription())
                .status(ReturnStatus.PENDING)
                .refundMethod(refundMethodEnum)
                .refundStatus(typeEnum == ReturnType.RETURN ? RefundStatus.PENDING : RefundStatus.NOT_REQUIRED)
                .staffNote(null)
                .bankName(refundMethodEnum == RefundMethod.BANK_TRANSFER ? request.getBankName() : null)
                .bankAccountNumber(
                        refundMethodEnum == RefundMethod.BANK_TRANSFER ? request.getBankAccountNumber() : null)
                .bankAccountHolder(
                        refundMethodEnum == RefundMethod.BANK_TRANSFER ? request.getBankAccountHolder() : null)
                .build();

        BigDecimal policyFactor = getRefundPolicyFactor(reasonEnum, days);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        for (ReturnItemRequest itemReq : request.getItems()) {
            OrderDetail orderDetail = orderDetailRepository.findById(itemReq.getOrderDetailId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

            if (!orderDetail.getOrder().getOrderId().equals(order.getOrderId())) {
                throw new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND);
            }

            if (itemReq.getQuantity() <= 0) {
                throw new AppException(ErrorCode.RETURN_QUANTITY_INVALID);
            }

            int alreadyReturned = returnItemRepository.sumQuantityByOrderDetailIdAndStatusIn(
                    orderDetail.getOrderDetailId(),
                    List.of(ReturnStatus.PENDING, ReturnStatus.APPROVED, ReturnStatus.COMPLETED));

            if (itemReq.getQuantity() > orderDetail.getQuantity() - alreadyReturned) {
                throw new AppException(ErrorCode.RETURN_QUANTITY_EXCEEDED);
            }

            BigDecimal itemRefund = BigDecimal.ZERO;

            if (typeEnum == ReturnType.RETURN) {
                BigDecimal orderTotal = order.getTotalAmount();
                BigDecimal discount = order.getDiscountAmount();

                BigDecimal allocatedDetailDiscount = (discount != null && orderTotal.compareTo(BigDecimal.ZERO) > 0)
                        ? discount.multiply(orderDetail.getTotalPrice()).divide(orderTotal, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                BigDecimal detailValueAfterVoucher = orderDetail.getTotalPrice().subtract(allocatedDetailDiscount);
                BigDecimal unitValueAfterVoucher = detailValueAfterVoucher
                        .divide(BigDecimal.valueOf(orderDetail.getQuantity()), 4, RoundingMode.HALF_UP);
                BigDecimal requestedValue = unitValueAfterVoucher.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

                itemRefund = requestedValue.multiply(policyFactor).setScale(2, RoundingMode.HALF_UP);
                totalRefundAmount = totalRefundAmount.add(itemRefund);
            }

            ReturnItem returnItem = ReturnItem.builder()
                    .returnRequest(returnRequest)
                    .orderDetail(orderDetail)
                    .quantity(itemReq.getQuantity())
                    .refundAmount(itemRefund)
                    .build();

            returnItems.add(returnItem);
        }

        returnRequest.setRefundAmount(totalRefundAmount.setScale(2, RoundingMode.HALF_UP));
        returnRequest.setReturnItems(returnItems);

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        if (typeEnum == ReturnType.EXCHANGE) {
            returnStockService.reserveStockForExchange(saved);
        }

        return returnRequestMapper.toReturnRequestResponse(saved);
    }

    @Override
    public ReturnRequestResponse uploadMedia(Long id, List<MultipartFile> files) {
        checkLogin();
        User currentUser = getCurrentUser();

        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (!returnRequest.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (returnRequest.getStatus() != ReturnStatus.PENDING) {
            throw new AppException(ErrorCode.RETURN_REQUEST_NOT_PENDING);
        }

        fileService.validateReturnImages(files);

        returnRequest.getMediaFiles().clear();

        for (MultipartFile file : files) {
            MediaFile mediaFile = fileService.uploadReturnImage(file);
            mediaFile.setReturnRequest(returnRequest);
            returnRequest.getMediaFiles().add(mediaFile);
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        return returnRequestMapper.toReturnRequestResponse(saved);
    }

    @Override
    public Page<ReturnRequestResponse> getMyReturnRequests(Pageable pageable) {
        checkLogin();
        User currentUser = getCurrentUser();
        return returnRequestRepository.findByRequestedBy_UserIdOrderByCreatedAtDesc(currentUser.getUserId(), pageable)
                .map(returnRequestMapper::toReturnRequestResponse);
    }

    @Override
    public ReturnRequestResponse getReturnRequestById(Long id) {
        checkLogin();
        User currentUser = getCurrentUser();

        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (currentUser.getRole() == Role.CUSTOMER) {
            if (!returnRequest.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

        return returnRequestMapper.toReturnRequestResponse(returnRequest);
    }

    @Override
    public ReturnRequestResponse cancelReturnRequest(Long id) {
        checkLogin();
        User currentUser = getCurrentUser();

        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (!returnRequest.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (returnRequest.getStatus() != ReturnStatus.PENDING) {
            throw new AppException(ErrorCode.RETURN_REQUEST_NOT_PENDING);
        }

        if (returnRequest.getType() == ReturnType.EXCHANGE) {
            returnStockService.releaseReservedStock(returnRequest);
        }

        returnRequest.setStatus(ReturnStatus.CANCELLED);
        if (returnRequest.getType() == ReturnType.RETURN) {
            returnRequest.setRefundStatus(RefundStatus.FAILED);
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        return returnRequestMapper.toReturnRequestResponse(saved);
    }

    @Override
    public Page<ReturnRequestResponse> getAllReturnRequests(ReturnFilterRequest filter, String sortBy, Pageable pageable) {
        checkLogin();
        Pageable resolvedPageable = buildPageable(pageable, sortBy);
        Specification<ReturnRequest> spec = buildReturnSpec(filter);
        return returnRequestRepository.findAll(spec, resolvedPageable)
                .map(returnRequestMapper::toReturnRequestResponse);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Specification<ReturnRequest> buildReturnSpec(ReturnFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String keyword = filter.getKeyword();
            if (keyword != null && !keyword.isBlank()) {
                String[] terms = keyword.trim().toLowerCase().split("\\s+");
                Join<Object, Object> orderJoin = root.join("order");
                Join<Object, Object> userJoin  = root.join("requestedBy");
                for (String term : terms) {
                    String pattern = "%" + term + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(orderJoin.get("orderCode")),  pattern),
                            cb.like(cb.lower(root.get("returnCode")),      pattern),
                            cb.like(cb.lower(userJoin.get("fullName")),    pattern),
                            cb.like(cb.lower(userJoin.get("email")),       pattern)
                    ));
                }
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getRefundMethod() != null) {
                predicates.add(cb.equal(root.get("refundMethod"), filter.getRefundMethod()));
            }
            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }

            if ((keyword == null || keyword.isBlank()) && filter.getOrderCode() != null && !filter.getOrderCode().isBlank()) {
                Join<Object, Object> orderJoin = root.join("order");
                predicates.add(cb.equal(orderJoin.get("orderCode"), filter.getOrderCode().trim()));
            }
            if (filter.getReturnCode() != null && !filter.getReturnCode().isBlank()) {
                predicates.add(cb.equal(root.get("returnCode"), filter.getReturnCode().trim()));
            }

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), filter.getFromDate().atStartOfDay()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThan(
                        root.get("createdAt"), filter.getToDate().plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable buildPageable(Pageable pageable, String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt_desc";
        }
        String[] parts = sortBy.split("_");
        String field = parts[0];
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Set<String> allowedFields = Set.of("createdAt", "status", "refundMethod", "type", "refundAmount");
        if (!allowedFields.contains(field)) {
            throw new AppException(ErrorCode.INVALID_SORT_OPTION);
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(direction, field));
    }

    @Override
    @Transactional
    public ReturnRequestResponse updateReturnStatus(Long id, UpdateReturnStatusRequest request) {
        checkLogin();
        User currentUser = getCurrentUser();

        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        ReturnStatus newStatus;
        try {
            newStatus = ReturnStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        ReturnStatus currentStatus = returnRequest.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        if (returnRequest.getType() == ReturnType.EXCHANGE) {
            switch (newStatus) {
                case APPROVED:
                    returnStockService.confirmStockForExchange(returnRequest);
                    break;
                case EXCHANGE_FAILED:
                    returnStockService.returnStockForExchange(returnRequest);
                    break;
                case REJECTED:
                case CANCELLED:
                    returnStockService.releaseReservedStock(returnRequest);
                    break;
                case COMPLETED:
                    break;
            }
        }

        if (returnRequest.getType() == ReturnType.RETURN) {
            switch (newStatus) {
                case COMPLETED:
                    paymentService.refundForReturn(returnRequest);
                    returnRequest.setRefundStatus(RefundStatus.SUCCESS);
                    break;
                case REJECTED:
                case CANCELLED:
                    returnRequest.setRefundStatus(RefundStatus.FAILED);
                    break;
            }
        }

        returnRequest.setStatus(newStatus);
        if (request.getStaffNote() != null) {
            returnRequest.setStaffNote(request.getStaffNote());
        }
        returnRequest.setProcessedBy(currentUser);
        returnRequest.setProcessedAt(LocalDateTime.now());

        if (newStatus == ReturnStatus.COMPLETED) {
            returnRequest.setCompletedAt(LocalDateTime.now());
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        return returnRequestMapper.toReturnRequestResponse(saved);
    }


    private void validateStatusTransition(ReturnStatus current, ReturnStatus next) {
        if (current == ReturnStatus.REJECTED || current == ReturnStatus.COMPLETED
                || current == ReturnStatus.CANCELLED || current == ReturnStatus.EXCHANGE_FAILED) {
            throw new AppException(ErrorCode.RETURN_REQUEST_ALREADY_PROCESSED);
        }

        if (current == ReturnStatus.PENDING) {
            if (next != ReturnStatus.APPROVED && next != ReturnStatus.REJECTED
                    && next != ReturnStatus.CANCELLED) {
                throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
            }
        } else if (current == ReturnStatus.APPROVED) {
            if (next != ReturnStatus.COMPLETED && next != ReturnStatus.EXCHANGE_FAILED) {
                throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
            }
        }
    }

    private BigDecimal getRefundPolicyFactor(ReturnReason reason, long days) {
        if (isStoreFault(reason)) {
            return BigDecimal.valueOf(1.0);
        } else {
            if (days <= 7) {
                return BigDecimal.valueOf(0.8);
            } else if (days <= 30) {
                return BigDecimal.valueOf(0.7);
            } else {
                throw new AppException(ErrorCode.RETURN_PERIOD_EXPIRED);
            }
        }
    }

    private boolean isStoreFault(ReturnReason reason) {
        return reason == ReturnReason.DAMAGED
                || reason == ReturnReason.WRONG_PRODUCT
                || reason == ReturnReason.MISSING_ITEM
                || reason == ReturnReason.EXPIRED
                || reason == ReturnReason.OUT_OF_STOCK;
    }

    private String generateReturnCode() {
        String code;
        do {
            code = "RTN" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (returnRequestRepository.existsByReturnCode(code));
        return code;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        String userId = authentication.getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void checkLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}