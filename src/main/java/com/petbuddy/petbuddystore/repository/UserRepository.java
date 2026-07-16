package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.StaffTask;
import com.petbuddy.petbuddystore.common.enums.UserStatus;
import com.petbuddy.petbuddystore.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    List<User> findAllByRole(Role role);
    List<User> findByStatusAndSuspendedUntilBefore(UserStatus status, LocalDateTime time);

    @Query("""
    SELECT u
    FROM User u
    WHERE ((:role IS NULL AND u.role IN ('MANAGER', 'STAFF')) OR u.role = :role)
        AND (:staffTask IS NULL OR u.staffTask = :staffTask)
    """)
    Page<User> findUsers(@Param("role") Role role, @Param("staffTask") StaffTask staffTask, Pageable pageable);
}
