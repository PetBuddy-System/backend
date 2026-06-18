package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.VoucherRequest;
import com.petbuddy.petbuddystore.dto.response.VoucherResponse;
import com.petbuddy.petbuddystore.model.Voucher;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    Voucher toVoucher(VoucherRequest request);

    VoucherResponse toVoucherResponse(Voucher voucher);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateVoucherFromRequest(VoucherRequest request, @MappingTarget Voucher voucher);
}
