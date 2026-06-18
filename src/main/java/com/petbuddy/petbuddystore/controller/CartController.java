package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.AddToCartRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateCartItemRequest;
import com.petbuddy.petbuddystore.dto.response.CartResponse;
import com.petbuddy.petbuddystore.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart API", description = "Quản lý giỏ hàng")
public class CartController {

    private final CartService cartService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/items")
    @Operation(description = "Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<ApiResponse<Void>> addToCart(@RequestBody @Valid AddToCartRequest request) {
        cartService.addToCart(request);
        return ResponseEntity.ok(
                ApiResponse.success("Product added to cart successfully", null));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Get current cart")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(
                ApiResponse.success("Cart retrieved successfully", cartService.getCart()));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Update product quantity in cart")
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> updateItemQuantity(@PathVariable UUID cartItemId,
                                                                @RequestBody @Valid UpdateCartItemRequest request){
        cartService.updateCart(cartItemId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Cart item quantity updated successfully", null));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Remove product from cart")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID productId) {

        cartService.removeItem(productId);

        return ResponseEntity.ok(
                ApiResponse.success("Product removed from cart successfully", null));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(description = "Clear cart")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {

        cartService.clearCart();

        return ResponseEntity.ok(
                ApiResponse.success("Cart cleared successfully", null));
    }
}
