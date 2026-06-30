package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.PaymentInitResponse;
import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(source = "order.orderId",   target = "orderId")
    @Mapping(source = "order.orderCode", target = "orderCode")
    PaymentResponse toPaymentResponse(Payment payment);

    @Mapping(source = "order.orderId",   target = "orderId")
    @Mapping(source = "order.orderCode", target = "orderCode")
    @Mapping(target = "clientSecret",    ignore = true)
    PaymentInitResponse toPaymentInitResponse(Payment payment);
}
