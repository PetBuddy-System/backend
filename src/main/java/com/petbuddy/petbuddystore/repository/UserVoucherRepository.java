package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.model.UserVouchers;
import com.petbuddy.petbuddystore.model.Voucher;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserVoucherRepository extends CrudRepository<UserVouchers, String> {
    long countByUserAndVoucher(User user, Voucher voucher);
}
