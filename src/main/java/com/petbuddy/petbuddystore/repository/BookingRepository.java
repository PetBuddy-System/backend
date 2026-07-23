package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import com.petbuddy.petbuddystore.model.Booking;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    @EntityGraph(attributePaths = {
            "user", "staffSchedule", "staffSchedule.staff",
            "bookingDetails", "bookingDetails.pet", "bookingDetails.catalog", "bookingDetails.timeSlot"
    })
    Optional<Booking> findWithDetailsByBookingId(Integer bookingId);

    @EntityGraph(attributePaths = {"bookingDetails", "bookingDetails.pet", "bookingDetails.catalog", "staffSchedule", "staffSchedule.staff"})
    List<Booking> findByUser_UserIdOrderByCreateAtDesc(String userId);

    @EntityGraph(attributePaths = {"bookingDetails", "staffSchedule", "staffSchedule.staff"})
    List<Booking> findByBookingStatusOrderByCreateAtAsc(BookingStatus bookingStatus);

    @EntityGraph(attributePaths = {"bookingDetails", "staffSchedule", "staffSchedule.staff"})
    List<Booking> findByStaffSchedule_Staff_UserIdOrderByCreateAtDesc(String staffId);

    @EntityGraph(attributePaths = {"bookingDetails", "staffSchedule", "staffSchedule.staff"})
    List<Booking> findByBookingStatusInAndScheduledAtBetweenOrderByCreateAtDesc(
            Collection<BookingStatus> statuses,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
        SELECT COUNT(bd)
        FROM BookingDetail bd
        JOIN bd.booking b
        WHERE bd.timeSlot.id = :timeSlotId
          AND b.scheduledAt >= :from
          AND b.scheduledAt < :to
          AND b.bookingStatus NOT IN :excludedStatuses
    """)
    long countBookedPetsInSlot(
            @Param("timeSlotId") Integer timeSlotId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excludedStatuses") Collection<BookingStatus> excludedStatuses
    );

    @EntityGraph(attributePaths = {"bookingDetails", "staffSchedule", "staffSchedule.staff"})
    List<Booking> findByStaffScheduleAndBookingStatusIn(StaffSchedule staffSchedule, Collection<BookingStatus> statuses);

    @EntityGraph(attributePaths = {"bookingDetails", "staffSchedule", "staffSchedule.staff"})
    List<Booking> findByStaffScheduleAndBookingStatusInAndScheduledAtBetween(
            StaffSchedule staffSchedule,
            Collection<BookingStatus> statuses,
            LocalDateTime from,
            LocalDateTime to
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"bookingDetails", "staffSchedule", "staffSchedule.staff"})
    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.scheduledAt BETWEEN :from AND :to
          AND b.bookingStatus IN :statuses
          AND b.staffSchedule IS NOT NULL
    """)
    List<Booking> findAutoReassignCandidatesForUpdate(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    boolean existsByStaffScheduleAndScheduledAtAndBookingStatusIn(
            StaffSchedule staffSchedule,
            LocalDateTime scheduledAt,
            Collection<BookingStatus> statuses
    );
}
