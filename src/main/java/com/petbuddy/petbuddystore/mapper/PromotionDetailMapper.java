package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.PromotionDetailResponse;
import com.petbuddy.petbuddystore.model.PromotionDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromotionDetailMapper {

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productCode", source = "product.productCode")
    @Mapping(target = "sale_price", source = "product.sale_price")
    @Mapping(target = "promotion_price", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    PromotionDetailResponse toPromotionDetailResponse(PromotionDetail detail);
}