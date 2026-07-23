package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import com.petbuddy.petbuddystore.model.BookingDetail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Integer> {
    @EntityGraph(attributePaths = {"booking", "booking.staffSchedule", "booking.staffSchedule.staff", "mediaFiles"})
    Optional<BookingDetail> findWithBookingByBookingDetailId(Integer bookingDetailId);

    @Query("""
        SELECT bd
        FROM BookingDetail bd
        JOIN FETCH bd.booking b
        WHERE bd.pet.petId = :petId
          AND b.scheduledAt >= :startOfDay
          AND b.scheduledAt < :endOfDay
          AND b.bookingStatus NOT IN :excludedStatuses
    """)
    java.util.List<BookingDetail> findActiveBookingsForPetOnDay(
            @Param("petId") String petId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("excludedStatuses") Collection<BookingStatus> excludedStatuses
    );
}
