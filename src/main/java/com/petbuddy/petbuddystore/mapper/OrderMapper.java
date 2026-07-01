package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.OrderDetailResponse;
import com.petbuddy.petbuddystore.dto.response.OrderResponse;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.OrderDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {PaymentMapper.class, VoucherMapper.class},componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "clientSecret", source = "payment.stripeClientSecret")
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "voucher", source = "voucher")
    OrderResponse toOrderResponse(Order order);
    OrderDetailResponse toOrderDetailResponse(OrderDetail orderDetail);
}
