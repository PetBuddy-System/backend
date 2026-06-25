package com.petbuddy.petbuddystore.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.AddToCartRequest;
import com.petbuddy.petbuddystore.dto.request.MergeCartRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateCartItemRequest;
import com.petbuddy.petbuddystore.dto.response.CartResponse;
import com.petbuddy.petbuddystore.mapper.CartMapper;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.CartService;
import com.petbuddy.petbuddystore.service.ProductService;
import com.petbuddy.petbuddystore.dto.cart.CartItemData;
import com.petbuddy.petbuddystore.dto.cart.CartData;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartServiceImpl implements CartService {

    UserRepository userRepository;
    ProductService productService;
    ProductBatchRepository productBatchRepository;
    CartMapper cartMapper;
    ObjectMapper objectMapper;

    @Override
    public void addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        List<CartItemData> items = loadItems(user);

        Product product = productService.getProductEntityById(request.getProductId());
        int availableStock = productBatchRepository.findAvailableStockByProductId(product.getProductId());

        CartItemData existingItem = items.stream()
                .filter(item -> item.getProductId().equals(product.getProductId()))
                .findFirst()
                .orElse(null);

        int newQuantity = request.getQuantity();
        if (existingItem != null) {
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
        } else {
            items.add(CartItemData.builder()
                    .cartItemId(UUID.randomUUID())
                    .productId(product.getProductId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(request.getQuantity())
                    .imageUrl(getFirstImage(product))
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build());
        }

        saveItems(user, items);
    }

    @Override
    public CartResponse getCart() {
        User user = getCurrentUser();
        List<CartItemData> items = loadItems(user);

        boolean changed = false;
        for (CartItemData item : items) {
            if (item.getCartItemId() == null) {
                item.setCartItemId(UUID.randomUUID());
                changed = true;
            }
        }
        if (changed) saveItems(user, items);

        return toResponse(user.getUserId(), items);
    }

    @Override
    public void removeItem(UUID productId) {
        User user = getCurrentUser();
        List<CartItemData> items = loadItems(user);

        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        saveItems(user, items);
    }

    @Override
    public void clearCart() {
        User user = getCurrentUser();
        user.setCartData(null);
        userRepository.save(user);
    }

    @Override
    public void updateCart(UUID cartItemId, UpdateCartItemRequest request) {
        User user = getCurrentUser();
        List<CartItemData> items = loadItems(user);

        CartItemData item = items.stream()
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

        saveItems(user, items);
    }

    @Override
    public CartResponse mergeCart(MergeCartRequest request) {
        User user = getCurrentUser();
        List<CartItemData> items = loadItems(user);

        if (request.getItems() != null) {
            for (AddToCartRequest guestItem : request.getItems()) {
                Product product = productService.getProductEntityById(guestItem.getProductId());
                int availableStock = productBatchRepository.findAvailableStockByProductId(product.getProductId());

                CartItemData existingItem = items.stream()
                        .filter(i -> i.getProductId().equals(guestItem.getProductId()))
                        .findFirst()
                        .orElse(null);

                int newQuantity = guestItem.getQuantity() + (existingItem != null ? existingItem.getQuantity() : 0);

                newQuantity = Math.min(newQuantity, availableStock);
                if (newQuantity <= 0) continue;

                if (existingItem != null) {
                    existingItem.setQuantity(newQuantity);
                    existingItem.setPrice(product.getPrice());
                    existingItem.setProductName(product.getName());
                    existingItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));
                } else {
                    items.add(CartItemData.builder()
                            .cartItemId(UUID.randomUUID())
                            .productId(product.getProductId())
                            .productName(product.getName())
                            .price(product.getPrice())
                            .quantity(newQuantity)
                            .imageUrl(getFirstImage(product))
                            .subtotal(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)))
                            .build());
                }
            }
        }

        saveItems(user, items);
        return toResponse(user.getUserId(), items);
    }


    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    private User getCurrentUser() {
        return userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }

    private List<CartItemData> loadItems(User user) {
        if (user.getCartData() == null || user.getCartData().isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(
                    user.getCartData(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CartItemData.class)
            );
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private void saveItems(User user, List<CartItemData> items) {
        try {
            user.setCartData(objectMapper.writeValueAsString(items));
            userRepository.save(user);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cart data", e);
        }
    }

    private CartResponse toResponse(String userId, List<CartItemData> items) {
        CartData cartData = CartData.builder().userId(userId).items(items).build();
        return cartMapper.toCartResponse(cartData);
    }
    private String getFirstImage(Product product) {
        if (product == null || product.getImageUrls() == null || product.getImageUrls().isEmpty()) {
            return null;
        }
        return product.getImageUrls().getFirst();
    }
}