package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import com.petbuddy.petbuddystore.model.WorkSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, String> {
    boolean existsByWorkDateAndStartTimeAndEndTime(LocalDate date, LocalTime start, LocalTime end);

    @Query("""
    SELECT DISTINCT ws
    FROM WorkSchedule ws
    LEFT JOIN FETCH ws.staffSchedules ss
    LEFT JOIN FETCH ss.staff
    WHERE ws.workScheduleId = :workScheduleId
""")
    Optional<WorkSchedule> findByIdWithStaffs(@Param("workScheduleId") String workScheduleId);

    @Query("""
        SELECT ws
        FROM WorkSchedule ws
        WHERE (:fromDate IS NULL OR ws.workDate >= :fromDate)
          AND (:toDate IS NULL OR ws.workDate <= :toDate)
          AND (:shiftType IS NULL OR ws.shiftType = :shiftType)
        ORDER BY ws.workDate DESC, ws.startTime ASC
    """)
    Page<WorkSchedule> findWorkSchedules(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                                   @Param("shiftType") ShiftType shiftType, Pageable pageable);


//
//    List<StaffSchedule> findByStaff_UserIdOrderByWorkDateAscStartTimeAsc(String staffId);
//    List<StaffSchedule> findByStaff_UserIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(String staffId, LocalDate fromDate, LocalDate toDate);
//
//    List<StaffSchedule> findByWorkDateOrderByStartTimeAsc(LocalDate workDate);
//    List<StaffSchedule> findByWorkDateBetweenOrderByWorkDateAscStartTimeAsc(LocalDate fromDate, LocalDate toDate);
}
