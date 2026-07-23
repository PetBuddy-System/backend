package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.PaymentResponse;
import com.petbuddy.petbuddystore.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(source = "order.orderId",   target = "orderId")
    @Mapping(source = "order.orderCode", target = "orderCode")
    @Mapping(source = "booking.bookingId", target = "bookingId")
    @Mapping(source = "booking.bookingCode", target = "bookingCode")
    PaymentResponse toPaymentResponse(Payment payment);
}
