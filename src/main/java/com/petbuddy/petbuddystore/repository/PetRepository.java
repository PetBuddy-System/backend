package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.PetProfile;
import com.petbuddy.petbuddystore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<PetProfile, String> {
    List<PetProfile> findByUser(User user);
}
