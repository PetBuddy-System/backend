package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.PromotionRequest;
import com.petbuddy.petbuddystore.dto.request.PromotionUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBaseResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionListResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionResponse;
import com.petbuddy.petbuddystore.model.Promotion;
import com.petbuddy.petbuddystore.model.PromotionDetail;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        uses = PromotionDetailMapper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PromotionMapper {

    PromotionListResponse toListPromotionResponse(Promotion promotion);

    @Mapping(target = "promotionDetails", source = "promotionDetails")
    PromotionResponse toPromotionResponse(Promotion promotion);

    @Mapping(target = "promotionDetails", ignore = true)
    Promotion toPromotion(PromotionRequest request);

    void updatePromotionFromRequest(PromotionUpdateRequest request, @MappingTarget Promotion promotion);

    @Mapping(target = "promotionDetails", expression = "java(clonePromotionDetails(promotion.getPromotionDetails()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Promotion clonePromotion(Promotion promotion);

    default List<PromotionDetail> clonePromotionDetails(List<PromotionDetail> details) {
        if (details == null) return null;
        return details.stream()
                .map(this::clonePromotionDetail)
                .collect(Collectors.toList());
    }

    default PromotionDetail clonePromotionDetail(PromotionDetail detail) {
        if (detail == null) return null;
        PromotionDetail cloned = new PromotionDetail();
        cloned.setPromotionDetailId(detail.getPromotionDetailId());
        cloned.setProduct(detail.getProduct());
        cloned.setPromotionType(detail.getPromotionType());
        cloned.setDiscountValue(detail.getDiscountValue());
        return cloned;
    }
}
