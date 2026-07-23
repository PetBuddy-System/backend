package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.ReturnItemResponse;
import com.petbuddy.petbuddystore.dto.response.ReturnRequestResponse;
import com.petbuddy.petbuddystore.model.ReturnItem;
import com.petbuddy.petbuddystore.model.ReturnRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {UserMapper.class}, componentModel = "spring")
public interface ReturnRequestMapper {

    @Mapping(target = "orderId", source = "order.orderId")
    @Mapping(target = "orderCode", source = "order.orderCode")
    @Mapping(target = "recipientName", source = "order.recipientName")
    @Mapping(target = "phoneNumber", source = "order.phoneNumber")
    @Mapping(target = "address", source = "order.address")
    @Mapping(target = "latitude", source = "order.latitude")
    @Mapping(target = "longitude", source = "order.longitude")
    ReturnRequestResponse toReturnRequestResponse(ReturnRequest returnRequest);

    @Mapping(target = "orderDetailId", source = "orderDetail.orderDetailId")
    @Mapping(target = "productName", source = "orderDetail.productName")
    @Mapping(target = "productImage", source = "orderDetail.productImage")
    ReturnItemResponse toReturnItemResponse(ReturnItem returnItem);
}