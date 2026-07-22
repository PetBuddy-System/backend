package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.StaffShiftRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffShiftRegistrationRepository extends JpaRepository<StaffShiftRegistration, String> {
}
