package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.CartItemResponse;
import com.petbuddy.petbuddystore.dto.response.CartResponse;
import com.petbuddy.petbuddystore.model.Cart;
import com.petbuddy.petbuddystore.model.CartItem;
import com.petbuddy.petbuddystore.model.MediaFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(source = "user.userId", target = "userId")
    CartResponse toCartResponse(Cart cart);

    @Mapping(source = "product.productId",  target = "productId")
    CartItemResponse toCartItemResponse(CartItem item);
}
