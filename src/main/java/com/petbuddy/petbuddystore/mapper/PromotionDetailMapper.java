package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.PromotionDetailResponse;
import com.petbuddy.petbuddystore.model.PromotionDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PromotionDetailMapper {

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productCode", source = "product.productCode")
    @Mapping(target = "salePrice", source = "product.salePrice")
    @Mapping(target = "promotionPrice", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    PromotionDetailResponse toPromotionDetailResponse(PromotionDetail detail);

    default PromotionDetail clonePromotionDetail(PromotionDetail detail) {
        if (detail == null) return null;
        PromotionDetail cloned = new PromotionDetail();
        cloned.setPromotionDetailId(detail.getPromotionDetailId());
        cloned.setProduct(detail.getProduct());
        cloned.setPromotionType(detail.getPromotionType());
        cloned.setDiscountValue(detail.getDiscountValue());
        return cloned;
    }

    default List<PromotionDetail> clonePromotionDetailList(List<PromotionDetail> details) {
        if (details == null) return null;
        return details.stream()
                .map(this::clonePromotionDetail)
                .collect(Collectors.toList());
    }
}