package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.PromotionRequest;
import com.petbuddy.petbuddystore.dto.request.PromotionUpdateRequest;
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

    @Mapping(target = "promotionCode", source = "promotionCode")
    PromotionListResponse toListPromotionResponse(Promotion promotion);

    @Mapping(target = "promotionCode", source = "promotionCode")
    @Mapping(target = "promotionDetails", source = "promotionDetails")
    PromotionResponse toPromotionResponse(Promotion promotion);

    @Mapping(target = "promotionDetails", ignore = true)
    @Mapping(target = "promotionCode", ignore = true)
    Promotion toPromotion(PromotionRequest request);

    @Mapping(target = "promotionDetails", ignore = true)
    @Mapping(target = "promotionCode", ignore = true)
    void updatePromotionFromRequest(PromotionUpdateRequest request, @MappingTarget Promotion promotion);

    @Mapping(target = "promotionDetails", expression = "java(promotionDetailMapper.clonePromotionDetailList(promotion.getPromotionDetails()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Promotion clonePromotion(Promotion promotion, @Context PromotionDetailMapper promotionDetailMapper);
}