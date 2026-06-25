package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.CartItemResponse;
import com.petbuddy.petbuddystore.dto.response.CartResponse;
import com.petbuddy.petbuddystore.dto.cart.CartItemData;
import com.petbuddy.petbuddystore.dto.cart.CartData;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CartMapper {
    CartResponse toCartResponse(CartData cartData);

    CartItemResponse toCartItemResponse(CartItemData item);
}
