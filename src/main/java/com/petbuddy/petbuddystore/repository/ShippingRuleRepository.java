package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.ShippingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingRuleRepository extends JpaRepository<ShippingRule,Long> {
    @Query("""
       SELECT s
       FROM ShippingRule s
       WHERE :distance >= s.minDistance
       AND :distance <= s.maxDistance
       """)
    Optional<ShippingRule> findRuleByDistance(@Param("distance") Double distance);

    @Query("""
       SELECT COUNT(s) > 0
       FROM ShippingRule s
       WHERE s.minDistance < :maxDistance
       AND s.maxDistance > :minDistance
       """)
    boolean existsOverlap(Double minDistance, Double maxDistance);
}
