package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.request.VoucherRequest;
import com.petbuddy.petbuddystore.dto.response.VoucherResponse;
import com.petbuddy.petbuddystore.model.Voucher;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    Voucher toVoucher(VoucherRequest request);

    @Mapping(target = "usedByCurrentUser", ignore = true)
    VoucherResponse toVoucherResponse(Voucher voucher);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateVoucherFromRequest(VoucherRequest request, @MappingTarget Voucher voucher);
}
