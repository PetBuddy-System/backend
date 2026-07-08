package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID>, JpaSpecificationExecutor<Promotion> {
    @Query("SELECT p FROM Promotion p LEFT JOIN FETCH p.promotionDetails WHERE p.promotionId = :id")
    Optional<Promotion> findByIdWithDetails(@Param("id") UUID id);

    boolean existsByPromotionCode(String promotionCode);

    Optional<Promotion> findByPromotionCode(String promotionCode);

    List<Promotion> findByPromotionCodeContaining(String promotionCode);
}
