package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.model.StaffShiftRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StaffShiftRegistrationRepository extends JpaRepository<StaffShiftRegistration, String> {
    @Query("""
        SELECT COUNT(r) > 0
        FROM StaffShiftRegistration r
        WHERE r.registrationPeriod.registrationPeriodId = :registrationPeriodId
          AND r.staff.userId = :staffId
    """)
    boolean existsMyRegistration(@Param("registrationPeriodId") String registrationPeriodId, @Param("staffId") String staffId);

    @Query("""
        SELECT r
        FROM StaffShiftRegistration r
        JOIN FETCH r.staff
        WHERE r.registrationPeriod.registrationPeriodId = :registrationPeriodId
          AND r.staff.userId = :staffId
        ORDER BY r.workDate ASC, r.preferredShift ASC
    """)
    List<StaffShiftRegistration> findMyRegistrations(@Param("registrationPeriodId") String registrationPeriodId, @Param("staffId") String staffId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM StaffShiftRegistration r
        WHERE r.registrationPeriod.registrationPeriodId = :registrationPeriodId
          AND r.staff.userId = :staffId
    """)
    void deleteMyRegistrations(@Param("registrationPeriodId") String registrationPeriodId, @Param("staffId") String staffId);

    @Query(
            value = """
            SELECT r
            FROM StaffShiftRegistration r
            JOIN r.staff s
            WHERE r.registrationPeriod.registrationPeriodId = :registrationPeriodId
              AND (:staffKeyword IS NULL
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :staffKeyword, '%'))
                   OR LOWER(s.userId) LIKE LOWER(CONCAT('%', :staffKeyword, '%')))
              AND (:workDate IS NULL OR r.workDate = :workDate)
              AND (:shiftType IS NULL OR r.preferredShift = :shiftType)
            ORDER BY r.workDate ASC, r.preferredShift ASC
        """,
            countQuery = """
            SELECT COUNT(r)
            FROM StaffShiftRegistration r
            JOIN r.staff s
            WHERE r.registrationPeriod.registrationPeriodId = :registrationPeriodId
              AND (:staffKeyword IS NULL
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :staffKeyword, '%'))
                   OR LOWER(s.userId) LIKE LOWER(CONCAT('%', :staffKeyword, '%')))
              AND (:workDate IS NULL OR r.workDate = :workDate)
              AND (:shiftType IS NULL OR r.preferredShift = :shiftType)
        """
    )
    Page<StaffShiftRegistration> findRegistrationsForManager(@Param("registrationPeriodId") String registrationPeriodId,
            @Param("staffKeyword") String staffKeyword, @Param("workDate") LocalDate workDate, @Param("shiftType") ShiftType shiftType, Pageable pageable);
}
