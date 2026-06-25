package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.CageSize;
import com.petbuddy.petbuddystore.common.enums.CageStatus;
import com.petbuddy.petbuddystore.model.Cage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CageRepository extends JpaRepository<Cage, Long> {
    long countByCageSize(CageSize cageSize);

    @Query("""
    SELECT c
    FROM Cage c
    WHERE (:cageSize IS NULL OR c.cageSize = :cageSize)
      AND (:cageStatus IS NULL OR c.cageStatus = :cageStatus)
    """)
    Page<Cage> findCages(CageSize cageSize, CageStatus cageStatus, Pageable pageable);
}
