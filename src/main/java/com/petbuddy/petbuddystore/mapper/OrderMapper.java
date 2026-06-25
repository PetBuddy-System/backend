package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.OrderDetailResponse;
import com.petbuddy.petbuddystore.dto.response.OrderResponse;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.OrderDetail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);
    OrderDetailResponse toOrderDetailResponse(OrderDetail orderDetail);
}
