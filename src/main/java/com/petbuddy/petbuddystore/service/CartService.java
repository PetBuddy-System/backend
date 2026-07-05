package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.AddToCartRequest;
import com.petbuddy.petbuddystore.dto.request.MergeCartRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateCartItemRequest;
import com.petbuddy.petbuddystore.dto.response.CartItemResponse;
import com.petbuddy.petbuddystore.dto.response.CartResponse;
import com.petbuddy.petbuddystore.model.User;

import java.util.UUID;

public interface CartService {
    void addToCart(AddToCartRequest request);

    CartResponse getCart();

    void removeItem(UUID productId);

    void clearCart();
    void clearCart(User user);
    CartItemResponse updateCart(UUID cartItemId, UpdateCartItemRequest request);
    CartResponse mergeCart(MergeCartRequest request);
}
