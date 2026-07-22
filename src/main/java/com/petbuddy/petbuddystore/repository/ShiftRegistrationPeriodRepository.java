package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.RegistrationPeriodStatus;
import com.petbuddy.petbuddystore.model.ShiftRegistrationPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ShiftRegistrationPeriodRepository extends JpaRepository<ShiftRegistrationPeriod, String> {
    @Query("""
    SELECT p
    FROM ShiftRegistrationPeriod p
    WHERE (:status IS NULL OR p.status = :status)
      AND (:fromDate IS NULL OR p.workFromDate >= :fromDate)
      AND (:toDate IS NULL OR p.workToDate <= :toDate)
    """)
    Page<ShiftRegistrationPeriod> findRegistrationPeriods(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                                          @Param("status") RegistrationPeriodStatus status, Pageable pageable);

    boolean existsByWorkFromDateLessThanEqualAndWorkToDateGreaterThanEqual(LocalDate workToDate, LocalDate workFromDate);

}
