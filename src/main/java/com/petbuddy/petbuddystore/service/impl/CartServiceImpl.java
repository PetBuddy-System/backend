package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.AddToCartRequest;
import com.petbuddy.petbuddystore.dto.request.MergeCartRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateCartItemRequest;
import com.petbuddy.petbuddystore.dto.response.CartItemResponse;
import com.petbuddy.petbuddystore.dto.response.CartResponse;
import com.petbuddy.petbuddystore.dto.response.ProductPublicResponse;
import com.petbuddy.petbuddystore.mapper.CartMapper;
import com.petbuddy.petbuddystore.model.Cart;
import com.petbuddy.petbuddystore.model.CartItem;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.CartRepository;
import com.petbuddy.petbuddystore.repository.ProductBatchRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.CartService;
import com.petbuddy.petbuddystore.service.ProductService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartServiceImpl implements CartService {

    UserRepository userRepository;
    CartRepository cartRepository;
    ProductService productService;
    ProductBatchRepository productBatchRepository;
    CartMapper cartMapper;

    @Override
    public void addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        Product product = productService.getProductEntityById(request.getProductId());

        ProductPublicResponse response = productService.getProduct(request.getProductId());
        CartItem existingItem = findItemByProduct(cart, product.getProductId());

        int newQuantity = request.getQuantity() + (existingItem != null ? existingItem.getQuantity() : 0);
        validateStock(product.getProductId(), newQuantity);

        if (existingItem != null) {
            BigDecimal unitPrice = existingItem.getSalePrice() != null
                            ? existingItem.getSalePrice() : existingItem.getPrice();

            existingItem.setQuantity(newQuantity);
            existingItem.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            cart.getCartItems().add(buildCartItem(cart, product,response,request.getQuantity()));
        }
        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser_UserId(user.getUserId()).orElse(null);
        if (cart == null) {
            return cartMapper.toCartResponse(new Cart());
        }
        return cartMapper.toCartResponse(cart);
    }

    @Override
    public void removeItem(UUID cartItemId) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        boolean removed = cart.getCartItems()
                .removeIf(item -> item.getCartItemId().equals(cartItemId));

        if (!removed) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        cartRepository.save(cart);
    }

    @Override
    public void clearCart() {
        User user = getCurrentUser();
        cartRepository.findByUser_UserId(user.getUserId()).ifPresent(cart -> {
            cart.getCartItems().clear();
            cartRepository.save(cart);
        });
    }

    @Override
    public CartItemResponse updateCart(UUID cartItemId, UpdateCartItemRequest request) {

        User user = getCurrentUser();

        Cart cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        CartItem item = cart.getCartItems().stream()
                .filter(i -> i.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        int available = productBatchRepository.findAvailableStockByProductId(
                item.getProduct().getProductId());

        boolean adjusted = false;
        int quantity = request.getQuantity();

        if (quantity > available) {
            quantity = available;
            adjusted = true;
        }

        BigDecimal unitPrice = item.getSalePrice() != null
                ? item.getSalePrice()
                : item.getPrice();

        item.setQuantity(quantity);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));

        cartRepository.save(cart);

        CartItemResponse response = cartMapper.toCartItemResponse(item);
        response.setAdjusted(adjusted);

        return response;
    }

    @Override
    public CartResponse mergeCart(MergeCartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        if (request.getItems() != null) {
            for (AddToCartRequest guestItem : request.getItems()) {
                Product product = productService.getProductEntityById(guestItem.getProductId());
                ProductPublicResponse response = productService.getProduct(guestItem.getProductId());
                int availableStock = productBatchRepository.findAvailableStockByProductId(product.getProductId());

                CartItem existingItem = findItemByProduct(cart, product.getProductId());

                int newQuantity = guestItem.getQuantity() + (existingItem != null ? existingItem.getQuantity() : 0);
                newQuantity = Math.min(newQuantity, availableStock);

                if (newQuantity <= 0) continue;

                if (existingItem != null) {
                    existingItem.setQuantity(newQuantity);
                    existingItem.setSubtotal(existingItem.getPrice().multiply(BigDecimal.valueOf(newQuantity)));
                } else {
                    cart.getCartItems().add(buildCartItem(cart, product,response,newQuantity));
                }
            }
        }

        cartRepository.save(cart);
        return cartMapper.toCartResponse(cart);
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .cartItems(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartItem buildCartItem(Cart cart, Product product, ProductPublicResponse response, Integer quantity) {

        BigDecimal unitPrice = response.getSalePrice() != null ? response.getSalePrice() : response.getPrice();

        return CartItem.builder()
                .cart(cart)
                .product(product)
                .productName(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .salePrice(response.getSalePrice())
                .quantity(quantity)
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .build();
    }

    private CartItem findItemByProduct(Cart cart, UUID productId) {
        return cart.getCartItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void validateStock(UUID productId, int requiredQuantity) {
        int available = productBatchRepository.findAvailableStockByProductId(productId);
        if (available < requiredQuantity) {
            throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findById(auth.getName())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }
}