package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.AddToCartRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateCartItemRequest;
import com.petbuddy.petbuddystore.dto.response.CartResponse;
import com.petbuddy.petbuddystore.mapper.CartMapper;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.service.CartService;
import com.petbuddy.petbuddystore.service.ProductService;
import com.petbuddy.petbuddystore.session.CartItemSession;
import com.petbuddy.petbuddystore.session.CartSession;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartServiceImpl implements CartService {

    CartSession cartSession;
    ProductService productService;
    ProductBatchRepository productBatchRepository;
    CartMapper cartMapper;

    @Override
    public void addToCart(AddToCartRequest request) {
        ensureCartSessionUser();

        Product product = productService.getProductEntityById(request.getProductId());

        int availableStock = productBatchRepository.findAvailableStockByProductId(product.getProductId());

        CartItemSession existingItem = cartSession.getItems()
                .stream()
                .filter(item -> item.getProductId().equals(product.getProductId()))
                .findFirst()
                .orElse(null);

        int newQuantity = request.getQuantity();

        if (existingItem != null) {
            if (existingItem.getCartItemId() == null) {
                existingItem.setCartItemId(UUID.randomUUID());
            }
            newQuantity += existingItem.getQuantity();
        }

        if (availableStock < newQuantity) {
            throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        if (existingItem != null) {
            existingItem.setQuantity(newQuantity);
            existingItem.setPrice(product.getPrice());
            existingItem.setProductName(product.getName());
            existingItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));
            return;
        }

        cartSession.getItems().add(
                CartItemSession.builder()
                        .cartItemId(UUID.randomUUID())
                        .productId(product.getProductId())
                        .productName(product.getName())
                        .price(product.getPrice())
                        .quantity(request.getQuantity())
                        .subtotal(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                        .build()
        );
    }

    @Override
    public CartResponse getCart() {
        ensureCartSessionUser();

        for (CartItemSession item : cartSession.getItems()) {
            if (item.getCartItemId() == null) {
                item.setCartItemId(UUID.randomUUID());
            }
        }

        return cartMapper.toCartResponse(cartSession);
    }

    @Override
    public void removeItem(UUID productId) {
        ensureCartSessionUser();

        boolean removed = cartSession.getItems()
                .removeIf(item -> item.getProductId().equals(productId));

        if (!removed) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
    }

    @Override
    public void clearCart() {
        ensureCartSessionUser();
        cartSession.getItems().clear();
    }

    @Override
    public void updateCart(UUID cartItemId, UpdateCartItemRequest request) {
        ensureCartSessionUser();

        CartItemSession item = cartSession.getItems()
                .stream()
                .filter(i -> i.getCartItemId() != null && i.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        Product product = productService.getProductEntityById(item.getProductId());
        int availableStock = productBatchRepository.findAvailableStockByProductId(product.getProductId());

        if (availableStock < request.getQuantity()) {
            throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        boolean priceChanged = item.getPrice().compareTo(product.getPrice()) != 0;

        if (priceChanged) {
            if (Boolean.FALSE.equals(request.getAcceptPriceChange())) {
                throw new AppException(ErrorCode.PRODUCT_PRICE_CHANGE);
            }

            item.setPrice(product.getPrice());
            item.setProductName(product.getName());
        }

        item.setQuantity(request.getQuantity());
        item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return authentication.getName();
    }

    private void ensureCartSessionUser() {
        String currentUserId = getCurrentUserId();

        if (cartSession.getUserId() == null) {
            cartSession.setUserId(currentUserId);
            return;
        }

        if (!cartSession.getUserId().equals(currentUserId)) {
            cartSession.setUserId(currentUserId);
            cartSession.getItems().clear();
        }
    }
}