package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.PetProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetRepository extends JpaRepository<PetProfile, String> {
}
