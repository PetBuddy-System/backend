package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.CalculateRefundRequest;
import com.petbuddy.petbuddystore.dto.request.CreateReturnRequest;
import com.petbuddy.petbuddystore.dto.request.ReturnFilterRequest;
import com.petbuddy.petbuddystore.dto.request.ReturnItemRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateReturnStatusRequest;
import com.petbuddy.petbuddystore.dto.response.*;
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

        long days = validateOrderForReturn(order, currentUser);

        ReturnReason reasonEnum;
        try {
            reasonEnum = ReturnReason.valueOf(request.getReason());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        BigDecimal policyFactor = getRefundPolicyFactor(reasonEnum, days);
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        List<ReturnItemResponse> itemsResponse = new ArrayList<>();

        for (ReturnItemRequest itemReq : request.getItems()) {
            OrderDetail orderDetail = orderDetailRepository.findById(itemReq.getOrderDetailId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

            validateReturnItem(orderDetail, order, itemReq.getQuantity());

            BigDecimal itemRefund = calculateItemRefund(orderDetail, order, itemReq.getQuantity(), policyFactor);
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

        long days = validateOrderForReturn(order, currentUser);

        ReturnType typeEnum;
        ReturnReason reasonEnum;
        RefundMethod refundMethodEnum;
        try {
            typeEnum = ReturnType.valueOf(request.getType());
            reasonEnum = ReturnReason.valueOf(request.getReason());
            if (request.getRefundMethod() == null || request.getRefundMethod().isBlank()) {
                refundMethodEnum = RefundMethod.STRIPE_PAYMENT;
            } else {
                refundMethodEnum = RefundMethod.valueOf(request.getRefundMethod());
            }
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

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        BigDecimal policyFactor = getRefundPolicyFactor(reasonEnum, days);
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
                .bankAccountNumber(refundMethodEnum == RefundMethod.BANK_TRANSFER ? request.getBankAccountNumber() : null)
                .bankAccountHolder(refundMethodEnum == RefundMethod.BANK_TRANSFER ? request.getBankAccountHolder() : null)
                .build();

        for (ReturnItemRequest itemReq : request.getItems()) {
            OrderDetail orderDetail = orderDetailRepository.findById(itemReq.getOrderDetailId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

            validateReturnItem(orderDetail, order, itemReq.getQuantity());

            BigDecimal itemRefund = BigDecimal.ZERO;
            if (typeEnum == ReturnType.RETURN) {
                itemRefund = calculateItemRefund(orderDetail, order, itemReq.getQuantity(), policyFactor);
                totalRefundAmount = totalRefundAmount.add(itemRefund);
            }

            returnItems.add(ReturnItem.builder()
                    .returnRequest(returnRequest)
                    .orderDetail(orderDetail)
                    .quantity(itemReq.getQuantity())
                    .refundAmount(itemRefund)
                    .build());
        }

        returnRequest.setRefundAmount(totalRefundAmount.setScale(2, RoundingMode.HALF_UP));
        returnRequest.setReturnItems(returnItems);

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
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
            throw new AppException(ErrorCode.CANCELLED_NOT_ALLOWED);
        }

        returnRequest.setStatus(ReturnStatus.CANCELLED);
        if (returnRequest.getType() == ReturnType.RETURN) {
            returnRequest.setRefundStatus(RefundStatus.FAILED);
        }

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        return returnRequestMapper.toReturnRequestResponse(saved);
    }

    @Override
    public ReturnManagementResponse getAllReturnRequests(
            ReturnFilterRequest filter, String sortBy, Pageable pageable) {

        User currentUser = getCurrentUser();
        Specification<ReturnRequest> spec = buildReturnSpec(filter);

        if (currentUser.getRole() == Role.STAFF && currentUser.getStaffTask() == StaffTask.SHIPPER) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("shipper").get("userId"), currentUser.getUserId()));
        }

        Page<ReturnRequestResponse> responsePage = returnRequestRepository
                .findAll(spec, buildPageable(pageable, sortBy))
                .map(returnRequestMapper::toReturnRequestResponse);

        ReturnStatisticsResponse statistics = ReturnStatisticsResponse.builder()
                .totalRequests(returnRequestRepository.count(spec))
                .assigned(returnRequestRepository.count(
                        spec.and((root, query, cb) -> cb.isNotNull(root.get("shipper")))))
                .pending(countByStatus(spec, ReturnStatus.PENDING))
                .approved(countByStatus(spec, ReturnStatus.APPROVED))
                .completed(countByStatus(spec, ReturnStatus.COMPLETED))
                .rejected(countByStatus(spec, ReturnStatus.REJECTED))
                .build();

        return ReturnManagementResponse.builder()
                .returns(responsePage)
                .statistics(statistics)
                .build();
    }

    @Override
    @Transactional
    public ReturnRequestResponse updateReturnStatusByManagement(Long id, UpdateReturnStatusRequest request) {
        checkLogin();
        User currentUser = getCurrentUser();
        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
        ReturnStatus newStatus = parseStatus(request.getStatus());
        validateManagementStatusTransition(returnRequest.getType(), returnRequest.getStatus(), newStatus);
        processManagementStatusChange(returnRequest, newStatus, currentUser);
        return returnRequestMapper.toReturnRequestResponse(returnRequestRepository.save(returnRequest));
    }

    @Override
    @Transactional
    public ReturnRequestResponse updateReturnStatusByShipper(Long id, UpdateReturnStatusRequest request) {
        checkLogin();
        User currentUser = getCurrentUser();
        ReturnRequest returnRequest = returnRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));
        ReturnStatus newStatus = parseStatus(request.getStatus());
        validateShipperStatusTransition(returnRequest.getType(), returnRequest.getStatus(), newStatus);
        processShipperStatusChange(returnRequest, newStatus, currentUser);
        return returnRequestMapper.toReturnRequestResponse(returnRequestRepository.save(returnRequest));
    }

    private void validateManagementStatusTransition(ReturnType type, ReturnStatus current, ReturnStatus next) {
        if (isFinalStatus(current)) throw new AppException(ErrorCode.RETURN_REQUEST_ALREADY_PROCESSED);

        switch (current) {
            case PENDING:
                if (next != ReturnStatus.APPROVED && next != ReturnStatus.REJECTED)
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case PICKUP_FAILED:
                if (next != ReturnStatus.CANCELLED && next != ReturnStatus.APPROVED)
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case RETURNED_TO_STORE:
                ReturnStatus allowed = (type == ReturnType.EXCHANGE) ? ReturnStatus.READY_TO_DELIVER : ReturnStatus.COMPLETED;
                if (next != allowed && next != ReturnStatus.REJECTED_RETURN_SHIPPING)
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case REJECTED_RETURN_SHIPPING:
                if (next != ReturnStatus.APPROVED)
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case DELIVERING_FAILED:
                if (next != ReturnStatus.COMPLETED && next != ReturnStatus.APPROVED)
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            default:
                throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void validateShipperStatusTransition(ReturnType type, ReturnStatus current, ReturnStatus next) {
        if (isFinalStatus(current)) throw new AppException(ErrorCode.RETURN_REQUEST_ALREADY_PROCESSED);

        switch (current) {
            case APPROVED:
                if (next != ReturnStatus.PICKING_UP) throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case PICKING_UP:
                if (next != ReturnStatus.PICKED_UP && next != ReturnStatus.PICKUP_FAILED)
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case PICKED_UP:
                if (next != ReturnStatus.RETURNED_TO_STORE) throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case PICKUP_FAILED:
                if (next != ReturnStatus.APPROVED) throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case READY_TO_DELIVER:
                if (next != ReturnStatus.DELIVERING && next != ReturnStatus.DELIVERING_FAILED)
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            case DELIVERING:
                if (next != ReturnStatus.COMPLETED && next != ReturnStatus.DELIVERING_FAILED)
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
                break;
            default:
                throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void processManagementStatusChange(ReturnRequest returnRequest,
                                               ReturnStatus newStatus, User currentUser) {
        boolean isExchange = returnRequest.getType() == ReturnType.EXCHANGE;
        returnRequest.setStatus(newStatus);

        switch (newStatus) {
            case APPROVED:
                returnRequest.setApprovedAt(LocalDateTime.now());
                returnRequest.setCoordinator(currentUser);
                if (isExchange) returnStockService.reserveStockForExchange(returnRequest);
                break;

            case REJECTED:
            case REJECTED_RETURN_SHIPPING:
                returnRequest.setRejectedAt(LocalDateTime.now());
                returnRequest.setCoordinator(currentUser);
                returnRequest.setRefundStatus(RefundStatus.FAILED);
                if (isExchange) returnStockService.releaseReservedStock(returnRequest);
                break;

            case COMPLETED:
                if (returnRequest.getRejectedAt() == null) {
                    if (isExchange) {
                        returnStockService.confirmStockForExchange(returnRequest);
                    } else {
                        paymentService.refundForReturn(returnRequest);
                        returnRequest.setRefundStatus(RefundStatus.SUCCESS);
                        applyReturnedQuantitiesToOrder(returnRequest);
                    }
                }
                returnRequest.setCompletedAt(LocalDateTime.now());
                break;

            case CANCELLED:
                returnRequest.setCancelledAt(LocalDateTime.now());
                returnRequest.setCoordinator(currentUser);
                returnRequest.setRefundStatus(RefundStatus.FAILED);
                if (isExchange) returnStockService.releaseReservedStock(returnRequest);
                break;

            default:
                break;
        }
    }

    private void processShipperStatusChange(ReturnRequest returnRequest,
                                            ReturnStatus newStatus, User currentUser) {
        switch (newStatus) {
            case PICKING_UP:
                returnRequest.setStatus(newStatus);
                returnRequest.setPickingUpAt(LocalDateTime.now());
                returnRequest.setShipper(currentUser);
                break;

            case PICKED_UP:
                returnRequest.setStatus(newStatus);
                returnRequest.setPickedUpAt(LocalDateTime.now());
                returnRequest.setShipper(currentUser);
                break;

            case PICKUP_FAILED:
                int pickupCount = returnRequest.getPickupFailedCount() + 1;
                returnRequest.setPickupFailedCount(pickupCount);
                returnRequest.setPickupFailedAt(LocalDateTime.now());
                returnRequest.setStatus(pickupCount >= 3 ? newStatus : ReturnStatus.PICKING_UP);
                if (pickupCount >= 3) returnRequest.setShipper(null);
                break;

            case RETURNED_TO_STORE:
                returnRequest.setStatus(newStatus);
                returnRequest.setReturnedToStoreAt(LocalDateTime.now());
                break;

            case DELIVERING:
                returnRequest.setStatus(newStatus);
                returnRequest.setDeliveringAt(LocalDateTime.now());
                break;

            case DELIVERING_FAILED:
                int deliverCount = returnRequest.getDeliveryFailedCount() + 1;
                returnRequest.setDeliveryFailedCount(deliverCount);
                returnRequest.setDeliveringFailedAt(LocalDateTime.now());
                returnRequest.setStatus(deliverCount >= 3 ? newStatus : ReturnStatus.READY_TO_DELIVER);
                if (deliverCount >= 3) returnRequest.setShipper(null);
                break;

            case COMPLETED:
                returnRequest.setStatus(newStatus);
                if (returnRequest.getType() == ReturnType.EXCHANGE)
                    returnStockService.confirmStockForExchange(returnRequest);
                returnRequest.setCompletedAt(LocalDateTime.now());
                break;

            default:
                break;
        }
    }



    private void applyReturnedQuantitiesToOrder(ReturnRequest returnRequest) {
        Order order = returnRequest.getOrder();

        BigDecimal originalOrderTotal = getOriginalTotal(order);
        BigDecimal originalDiscount = getOriginalDiscount(order);

        for (ReturnItem item : returnRequest.getReturnItems()) {
            OrderDetail detail = item.getOrderDetail();

            int newQty = detail.getQuantity() - item.getQuantity();
            if (newQty < 0) {
                throw new AppException(ErrorCode.RETURN_QUANTITY_EXCEEDED);
            }

            BigDecimal effectivePrice = detail.getSalePrice() != null
                    ? detail.getSalePrice()
                    : detail.getUnitPrice();

            BigDecimal newLineTotal = effectivePrice
                    .multiply(BigDecimal.valueOf(newQty))
                    .setScale(2, RoundingMode.HALF_UP);

            detail.setQuantity(newQty);
            detail.setTotalPrice(newLineTotal);

            orderDetailRepository.save(detail);
        }

        // Tính lại tổng tiền hàng
        BigDecimal newOrderTotal = order.getOrderDetails().stream()
                .map(OrderDetail::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        order.setTotalAmount(newOrderTotal);

        // Phân bổ lại voucher theo tỷ lệ giống calculateRefund()
        BigDecimal remainingDiscount = BigDecimal.ZERO;

        if (originalDiscount.compareTo(BigDecimal.ZERO) > 0
                && originalOrderTotal.compareTo(BigDecimal.ZERO) > 0) {

            for (OrderDetail detail : order.getOrderDetails()) {

                if (detail.getQuantity() <= 0) {
                    continue;
                }

                BigDecimal allocatedDiscount = originalDiscount
                        .multiply(detail.getTotalPrice())
                        .divide(originalOrderTotal, 4, RoundingMode.HALF_UP);

                remainingDiscount = remainingDiscount.add(allocatedDiscount);
            }
        }

        remainingDiscount = remainingDiscount.setScale(2, RoundingMode.HALF_UP);
        order.setDiscountAmount(remainingDiscount);

        order.setFinalAmount(
                newOrderTotal
                        .subtract(remainingDiscount)
                        .add(order.getShippingFee())
                        .subtract(order.getShippingDiscountAmount())
                        .setScale(2, RoundingMode.HALF_UP)
        );

        orderRepository.save(order);
    }

    private long countByStatus(Specification<ReturnRequest> base, ReturnStatus status) {
        return returnRequestRepository.count(
                base.and((root, query, cb) -> cb.equal(root.get("status"), status)));
    }

    private long validateOrderForReturn(Order order, User currentUser) {
        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AppException(ErrorCode.ORDER_NOT_OWNED);
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.ORDER_NOT_COMPLETED);
        }
        long days = ChronoUnit.DAYS.between(order.getUpdatedAt().toLocalDate(), LocalDate.now());
        if (days > 30) {
            throw new AppException(ErrorCode.RETURN_PERIOD_EXPIRED);
        }
        return days;
    }

    private void validateReturnItem(OrderDetail orderDetail, Order order, int quantity) {
        if (!orderDetail.getOrder().getOrderId().equals(order.getOrderId())) {
            throw new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND);
        }
        if (quantity <= 0) {
            throw new AppException(ErrorCode.RETURN_QUANTITY_INVALID);
        }
        int alreadyReturned = returnItemRepository.sumQuantityByOrderDetailIdAndStatusIn(
                orderDetail.getOrderDetailId(),
                List.of(ReturnStatus.PENDING, ReturnStatus.APPROVED, ReturnStatus.COMPLETED));
        if (quantity > orderDetail.getQuantity() - alreadyReturned) {
            throw new AppException(ErrorCode.RETURN_QUANTITY_EXCEEDED);
        }
    }

    private BigDecimal calculateItemRefund(OrderDetail orderDetail, Order order,
                                           int quantity, BigDecimal policyFactor) {
        BigDecimal orderTotal = getOriginalTotal(order);
        BigDecimal discount  = getOriginalDiscount(order);

        BigDecimal allocatedDetailDiscount = (discount != null && orderTotal.compareTo(BigDecimal.ZERO) > 0)
                ? discount.multiply(orderDetail.getTotalPrice()).divide(orderTotal, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal detailValueAfterVoucher = orderDetail.getTotalPrice().subtract(allocatedDetailDiscount);
        BigDecimal unitValueAfterVoucher   = detailValueAfterVoucher
                .divide(BigDecimal.valueOf(orderDetail.getQuantity()), 4, RoundingMode.HALF_UP);
        BigDecimal requestedValue = unitValueAfterVoucher.multiply(BigDecimal.valueOf(quantity));

        return requestedValue.multiply(policyFactor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getOriginalDiscount(Order order) {
        return order.getOriginalDiscountAmount() != null
                ? order.getOriginalDiscountAmount()
                : Optional.ofNullable(order.getDiscountAmount())
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getOriginalTotal(Order order) {
        return order.getOriginalTotalAmount() != null
                ? order.getOriginalTotalAmount()
                : Optional.ofNullable(order.getTotalAmount())
                .orElse(BigDecimal.ZERO);
    }

    private ReturnStatus parseStatus(String status) {
        try {
            return ReturnStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private boolean isFinalStatus(ReturnStatus status) {
        return status == ReturnStatus.REJECTED
                || status == ReturnStatus.COMPLETED
                || status == ReturnStatus.CANCELLED;
    }

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