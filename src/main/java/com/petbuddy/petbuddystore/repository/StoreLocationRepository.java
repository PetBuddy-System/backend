package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.StoreLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreLocationRepository extends JpaRepository<StoreLocation, Long> {
    Optional<StoreLocation> findByActiveTrue();
    List<StoreLocation> findAllByOrderByCreatedAtDesc();
}
