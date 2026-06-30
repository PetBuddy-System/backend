package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.PromotionRequest;
import com.petbuddy.petbuddystore.dto.request.PromotionUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductBaseResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionListResponse;
import com.petbuddy.petbuddystore.dto.response.PromotionResponse;
import com.petbuddy.petbuddystore.model.Promotion;
import com.petbuddy.petbuddystore.model.PromotionDetail;
import org.mapstruct.*;

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

    @Mapping(target = "promotionId", source = "promotion.promotionId")
    @Mapping(target = "promotionName", source = "promotion.name")
    @Mapping(target = "discountType", source = "detail.discountType")
    @Mapping(target = "discountValue", source = "detail.discountValue")
    @Mapping(target = "promotionEndDate", source = "promotion.endDate")
    void updatePromotionInfo(@MappingTarget ProductBaseResponse response, Promotion promotion, PromotionDetail detail);
}
