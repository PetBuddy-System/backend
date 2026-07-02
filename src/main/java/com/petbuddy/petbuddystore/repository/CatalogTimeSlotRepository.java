package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.CatalogTimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface CatalogTimeSlotRepository extends JpaRepository<CatalogTimeSlot, Integer> {
    List<CatalogTimeSlot> findByCatalogCatalogIdOrderByDayOfWeekAscStartTimeAsc(Integer catalogId);

    List<CatalogTimeSlot> findByCatalogCatalogIdAndDayOfWeekOrderByStartTimeAsc(
            Integer catalogId,
            DayOfWeek dayOfWeek
    );

    List<CatalogTimeSlot> findByCatalogCatalogIdAndDayOfWeekAndIsActiveTrueOrderByStartTimeAsc(
            Integer catalogId,
            DayOfWeek dayOfWeek
    );

    boolean existsByCatalogCatalogIdAndDayOfWeekAndStartTime(
            Integer catalogId,
            DayOfWeek dayOfWeek,
            LocalTime startTime
    );
}
