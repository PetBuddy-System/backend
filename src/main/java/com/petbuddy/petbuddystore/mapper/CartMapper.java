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
    @Mapping(source = "product.name",        target = "productName")
    @Mapping(source = "product.price",       target = "price")
    @Mapping(source = "product.mediaFiles", target = "imageUrl", qualifiedByName = "firstImage")
    CartItemResponse toCartItemResponse(CartItem item);

    @Named("firstImage")
    default String firstImage(List<MediaFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return null;
        }
        return mediaFiles.getFirst().getFileUrl();
    }
}
