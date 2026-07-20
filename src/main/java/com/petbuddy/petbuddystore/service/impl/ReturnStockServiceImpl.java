package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.enums.ReturnStatus;
import com.petbuddy.petbuddystore.common.enums.ReturnType;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.RestockBatchRequest;
import com.petbuddy.petbuddystore.dto.request.RestockReturnRequest;
import com.petbuddy.petbuddystore.dto.response.RestockReturnResponse;
import com.petbuddy.petbuddystore.dto.response.RestockItemResponse;
import com.petbuddy.petbuddystore.dto.response.RestockBatchResponse;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.OrderBatchLocationRepository;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.repository.ReturnReservedStockRepository;
import com.petbuddy.petbuddystore.repository.ReturnRequestRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.ReturnStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnStockServiceImpl implements ReturnStockService {

    private final ProductBatchRepository productBatchRepository;
    private final ReturnReservedStockRepository returnReservedStockRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderBatchLocationRepository orderBatchLocationRepository;
    private final UserRepository userRepository;

    // ============ EXCHANGE (Đổi hàng) ============

    @Override
    public void validateStockForExchange(ReturnItem returnItem) {
        Product product = returnItem.getOrderDetail().getProduct();
        int quantity = returnItem.getQuantity();

        if (product.getStatus() != ProductStatus.ACTIVE) {
            log.warn("Sản phẩm {} đã ngừng kinh doanh, không thể đổi", product.getProductId());
            throw new AppException(ErrorCode.PRODUCT_DISCONTINUED);
        }

        List<ProductBatch> availableBatches = productBatchRepository
                .findActiveBatchesForUpdate(product.getProductId(), ProductStatus.ACTIVE);

        if (availableBatches.isEmpty()) {
            log.warn("Sản phẩm {} không còn batch nào để đổi", product.getProductId());
            throw new AppException(ErrorCode.PRODUCT_DISCONTINUED);
        }

        int totalAvailable = availableBatches.stream()
                .mapToInt(ProductBatch::getStockQuantity)
                .sum();

        if (totalAvailable < quantity) {
            log.warn("Sản phẩm {} không đủ stock (cần {}, có {})",
                    product.getProductId(), quantity, totalAvailable);
            throw new AppException(ErrorCode.PRODUCT_DISCONTINUED);
        }

        log.debug("Stock OK cho sản phẩm {}: cần {}, có {}",
                product.getProductId(), quantity, totalAvailable);
    }

    @Override
    @Transactional
    public void reserveStockForExchange(ReturnRequest returnRequest) {
        List<ReturnReservedStock> reservedList = new ArrayList<>();

        for (ReturnItem returnItem : returnRequest.getReturnItems()) {
            Product product = returnItem.getOrderDetail().getProduct();
            int quantity = returnItem.getQuantity();

            List<ProductBatch> batches = productBatchRepository
                    .findActiveBatchesForUpdate(product.getProductId(), ProductStatus.ACTIVE);

            int remaining = quantity;
            for (ProductBatch batch : batches) {
                if (remaining <= 0) break;

                int reserve = Math.min(remaining, batch.getStockQuantity());
                batch.setStockQuantity(batch.getStockQuantity() - reserve);
                remaining -= reserve;
                productBatchRepository.save(batch);

                ReturnReservedStock reserved = ReturnReservedStock.builder()
                        .returnItem(returnItem)
                        .batch(batch)
                        .quantity(reserve)
                        .reservedAt(LocalDateTime.now())
                        .expiredAt(LocalDateTime.now().plusHours(24))
                        .restockedQuantity(0)
                        .build();
                reservedList.add(reserved);

                log.debug("Đã reserve {} sản phẩm từ batch {} cho returnItem {}",
                        reserve, batch.getBatchCode(), returnItem.getReturnItemId());
            }

            if (remaining > 0) {
                for (ReturnReservedStock rs : reservedList) {
                    ProductBatch batch = rs.getBatch();
                    batch.setStockQuantity(batch.getStockQuantity() + rs.getQuantity());
                    productBatchRepository.save(batch);
                }
                log.error("Không đủ stock để reserve cho returnItem {}", returnItem.getReturnItemId());
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }
        }

        returnReservedStockRepository.saveAll(reservedList);
        log.info("Đã giữ hàng thành công cho exchange request {}", returnRequest.getReturnCode());
    }

    @Override
    @Transactional
    public void confirmStockForExchange(ReturnRequest returnRequest) {
        List<ReturnReservedStock> reserved = returnReservedStockRepository
                .findByReturnItem_ReturnRequest(returnRequest);

        if (reserved.isEmpty()) {
            log.warn("Không tìm thấy reserve stock cho return request {}", returnRequest.getReturnCode());
            return;
        }

        returnReservedStockRepository.deleteAll(reserved);
        log.info("Đã confirm stock cho exchange request {} (đã xóa {} bản ghi reserve)",
                returnRequest.getReturnCode(), reserved.size());
    }

    @Override
    @Transactional
    public void releaseReservedStock(ReturnRequest returnRequest) {
        List<ReturnReservedStock> reserved = returnReservedStockRepository
                .findByReturnItem_ReturnRequest(returnRequest);

        if (reserved.isEmpty()) {
            log.warn("Không tìm thấy reserve stock để giải phóng cho return request {}",
                    returnRequest.getReturnCode());
            return;
        }

        for (ReturnReservedStock rs : reserved) {
            ProductBatch batch = rs.getBatch();
            batch.setStockQuantity(batch.getStockQuantity() + rs.getQuantity());
            productBatchRepository.save(batch);
            log.debug("Đã trả {} sản phẩm vào batch {} cho returnItem {}",
                    rs.getQuantity(), batch.getBatchCode(), rs.getReturnItem().getReturnItemId());
        }

        returnReservedStockRepository.deleteAll(reserved);
        log.info("Đã giải phóng {} bản ghi reserve cho exchange request {}",
                reserved.size(), returnRequest.getReturnCode());
    }

    @Override
    @Transactional
    public void returnStockForExchange(ReturnRequest returnRequest) {
        for (ReturnItem returnItem : returnRequest.getReturnItems()) {
            Product product = returnItem.getOrderDetail().getProduct();
            int quantity = returnItem.getQuantity();

            List<ProductBatch> batches = productBatchRepository
                    .findActiveBatchesForUpdate(product.getProductId(), ProductStatus.ACTIVE);

            if (batches.isEmpty()) {
                ProductBatch newBatch = ProductBatch.builder()
                        .product(product)
                        .batchCode(generateBatchCode())
                        .stockQuantity(quantity)
                        .status(ProductStatus.ACTIVE)
                        .build();
                productBatchRepository.save(newBatch);
                log.info("Đã tạo batch mới {} cho sản phẩm {} với số lượng {}",
                        newBatch.getBatchCode(), product.getProductId(), quantity);
            } else {
                ProductBatch batch = batches.get(0);
                batch.setStockQuantity(batch.getStockQuantity() + quantity);
                productBatchRepository.save(batch);
                log.debug("Đã cộng {} sản phẩm vào batch {} cho sản phẩm {}",
                        quantity, batch.getBatchCode(), product.getProductId());
            }
        }
        log.info("Đã trả lại stock cho exchange request {} do giao hàng thất bại",
                returnRequest.getReturnCode());
    }

    // ============ RETURN (Hoàn tiền) ============

    @Override
    public RestockReturnResponse getRestockInfo(Long returnRequestId) {
        ReturnRequest returnRequest = returnRequestRepository.findByIdWithItems(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (returnRequest.getType() != ReturnType.RETURN) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        List<ReturnReservedStock> allReserved = returnReservedStockRepository
                .findByReturnItem_ReturnRequest(returnRequest);

        Map<String, ReturnReservedStock> reservedMap = allReserved.stream()
                .filter(rs -> rs.getRestockedQuantity() != null && rs.getRestockedQuantity() > 0)
                .collect(Collectors.toMap(
                        rs -> rs.getBatch().getBatchId().toString()
                                + "_" + rs.getReturnItem().getOrderDetail().getOrderDetailId().toString(),
                        rs -> rs,
                        (existing, replacement) -> existing
                ));

        List<RestockItemResponse> items = returnRequest.getReturnItems().stream()
                .map(returnItem -> {
                    OrderDetail orderDetail = returnItem.getOrderDetail();

                    List<OrderBatchLocation> deductedBatches =
                            orderBatchLocationRepository.findByOrderDetail(orderDetail);

                    // Tổng số lượng đã nhập của ReturnItem này
                    int totalRestockedForItem = allReserved.stream()
                            .filter(rs -> rs.getReturnItem().getReturnItemId().equals(returnItem.getReturnItemId()))
                            .mapToInt(rs -> rs.getRestockedQuantity() != null ? rs.getRestockedQuantity() : 0)
                            .sum();

                    int remainingCanRestock = Math.max(
                            returnItem.getQuantity() - totalRestockedForItem,
                            0
                    );

                    Set<String> seenBatchIds = new HashSet<>();
                    List<OrderBatchLocation> uniqueBatches = deductedBatches.stream()
                            .filter(obl -> seenBatchIds.add(obl.getBatch().getBatchId().toString()))
                            .toList();

                    List<RestockBatchResponse> batchResponses = uniqueBatches.stream()
                            .map(obl -> {

                                String key = obl.getBatch().getBatchId().toString()
                                        + "_" + orderDetail.getOrderDetailId();

                                ReturnReservedStock reserved = reservedMap.get(key);

                                int deductedQuantity = deductedBatches.stream()
                                        .filter(b -> b.getBatch().getBatchId().equals(obl.getBatch().getBatchId()))
                                        .mapToInt(OrderBatchLocation::getQuantity)
                                        .sum();

                                int restockedInBatch =
                                        reserved != null && reserved.getRestockedQuantity() != null
                                                ? reserved.getRestockedQuantity()
                                                : 0;

                                int availableToRestock = Math.min(
                                        deductedQuantity - restockedInBatch,
                                        remainingCanRestock
                                );

                                RestockBatchResponse.RestockBatchResponseBuilder builder =
                                        RestockBatchResponse.builder()
                                                .batchId(obl.getBatch().getBatchId())
                                                .batchCode(obl.getBatch().getBatchCode())
                                                .deductedQuantity(deductedQuantity)
                                                .availableToRestock(Math.max(availableToRestock, 0))
                                                .restockQuantity(restockedInBatch);

                                if (reserved != null && restockedInBatch > 0) {
                                    builder.restockedAt(reserved.getRestockedAt());
                                    builder.restockedBy(reserved.getRestockedBy());
                                }

                                return builder.build();
                            })
                            .toList();

                    return RestockItemResponse.builder()
                            .orderDetailId(orderDetail.getOrderDetailId())
                            .productName(orderDetail.getProductName())
                            .productImage(orderDetail.getProductImage())
                            .requestedQuantity(returnItem.getQuantity())
                            .batches(batchResponses)
                            .build();
                })
                .collect(Collectors.toList());

        return RestockReturnResponse.builder()
                .returnRequestId(returnRequestId)
                .returnCode(returnRequest.getReturnCode())
                .items(items)
                .build();
    }
    @Override
    @Transactional
    public void processRestock(Long returnRequestId, RestockReturnRequest request) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (returnRequest.getType() != ReturnType.RETURN) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (returnRequest.getStatus() != ReturnStatus.COMPLETED) {
            throw new AppException(ErrorCode.RESTOCK_ONLY_AFTER_COMPLETED);
        }

        if (returnRequest.getRestockedAt() != null) {
            throw new AppException(ErrorCode.ALREADY_RESTOCKED);
        }

        User currentUser = getCurrentUser();

        // Nhập kho
        for (var itemReq : request.getItems()) {
            ReturnItem returnItem = returnRequest.getReturnItems().stream()
                    .filter(ri -> ri.getOrderDetail().getOrderDetailId().equals(itemReq.getOrderDetailId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

            int totalRestock = itemReq.getBatches().stream()
                    .mapToInt(RestockBatchRequest::getRestockQuantity)
                    .sum();

            if (totalRestock > returnItem.getQuantity()) {
                throw new AppException(ErrorCode.RESTOCK_QUANTITY_EXCEEDED);
            }

            if (totalRestock == 0) {
                throw new AppException(ErrorCode.RESTOCK_QUANTITY_REQUIRED);
            }

            for (var batchReq : itemReq.getBatches()) {
                if (batchReq.getRestockQuantity() > 0) {
                    ProductBatch batch = productBatchRepository.findById(batchReq.getBatchId())
                            .orElseThrow(() -> new AppException(ErrorCode.BATCH_NOT_FOUND));

                    // 1. Cập nhật stock
                    batch.setStockQuantity(batch.getStockQuantity() + batchReq.getRestockQuantity());
                    productBatchRepository.save(batch);

                    // 2. Lưu thông tin nhập kho vào ReturnReservedStock
                    ReturnReservedStock reserved = returnReservedStockRepository
                            .findByReturnItemAndBatch(returnItem, batch)
                            .orElseGet(() -> {
                                // Nếu chưa có thì tạo mới
                                ReturnReservedStock newReserved = ReturnReservedStock.builder()
                                        .returnItem(returnItem)
                                        .batch(batch)
                                        .quantity(batchReq.getRestockQuantity())
                                        .reservedAt(LocalDateTime.now())
                                        .expiredAt(LocalDateTime.now().plusHours(24))
                                        .restockedQuantity(0)
                                        .build();
                                return returnReservedStockRepository.save(newReserved);
                            });

                    // Cập nhật số lượng đã nhập (cộng dồn)
                    int currentRestocked = reserved.getRestockedQuantity() != null ? reserved.getRestockedQuantity() : 0;
                    reserved.setRestockedQuantity(currentRestocked + batchReq.getRestockQuantity());
                    reserved.setRestockedAt(LocalDateTime.now());
                    reserved.setRestockedBy(currentUser.getFullName());
                    returnReservedStockRepository.save(reserved);

                    log.info("Đã nhập {} sản phẩm vào batch {} cho return request {} (tổng đã nhập: {})",
                            batchReq.getRestockQuantity(), batch.getBatchCode(),
                            returnRequestId, currentRestocked + batchReq.getRestockQuantity());
                }
            }
        }

        returnRequest.setRestockedAt(LocalDateTime.now());
        returnRequestRepository.save(returnRequest);

        log.info("Đã nhập kho thành công cho return request {} (vẫn giữ status {})",
                returnRequestId, returnRequest.getStatus());
    }

    // ============ Helper Methods ============

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String userId = authentication.getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private String generateBatchCode() {
        return "BATCH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}