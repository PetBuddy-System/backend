package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, String> {
    boolean existsByStaff_UserIdAndWorkSchedule_WorkScheduleId(String staffId, String workScheduleId);

    @Query("""
        SELECT COUNT(ss) > 0
        FROM StaffSchedule ss
        JOIN ss.workSchedule ws
        WHERE ss.staff.userId = :staffId
          AND ws.workDate = :workDate
          AND (:currentWorkScheduleId IS NULL OR ws.workScheduleId <> :currentWorkScheduleId)
          AND ss.scheduleStatus IN :statuses
          AND ws.startTime < :endTime
          AND ws.endTime > :startTime
    """)
    boolean existsSchedule(@Param("staffId") String staffId, @Param("workDate") LocalDate workDate,
            @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime,
            @Param("currentWorkScheduleId") String currentWorkScheduleId, @Param("statuses") List<ScheduleStatus> statuses);


    @Query("""
        SELECT COUNT(ss) > 0
        FROM StaffSchedule ss
        WHERE ss.workSchedule.workScheduleId = :workScheduleId
          AND ss.scheduleStatus IN :statuses
    """)
    boolean existsByWorkScheduleIdAndStatusIn(@Param("workScheduleId") String workScheduleId, @Param("statuses") List<ScheduleStatus> statuses);


    @Query("""
        SELECT ss
        FROM StaffSchedule ss
        JOIN FETCH ss.workSchedule ws
        JOIN FETCH ss.staff s
        WHERE s.userId = :staffId
          AND (:fromDate IS NULL OR ws.workDate >= :fromDate)
          AND (:toDate IS NULL OR ws.workDate <= :toDate)
          AND (:scheduleStatus IS NULL OR ss.scheduleStatus = :scheduleStatus)
        ORDER BY ws.workDate ASC, ws.startTime ASC
    """)
    List<StaffSchedule> findMySchedules(@Param("staffId") String staffId, @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate, @Param("scheduleStatus") ScheduleStatus scheduleStatus);


    @Query("""
        SELECT ss
        FROM StaffSchedule ss
        JOIN FETCH ss.workSchedule ws
        JOIN FETCH ss.staff s
        WHERE ss.staffScheduleId = :staffScheduleId
    """)
    Optional<StaffSchedule> findByIdWithWorkScheduleAndStaff(@Param("staffScheduleId") String staffScheduleId);
}
