package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    boolean existsByVoucherCode(String voucherCode);
     boolean existsByVoucherCodeAndVoucherIdNot(String voucherCode, UUID voucherId);
     Optional<Voucher> findByVoucherCode(String voucherCode);

}
