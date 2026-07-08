package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.AuditAction;
import com.petbuddy.petbuddystore.common.enums.AuditEntityType;
import com.petbuddy.petbuddystore.model.AuditLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(AuditEntityType entityType, UUID entityId);

    List<AuditLog> findByEntityIdOrderByPerformedAtDesc(UUID entityId);

    @Query("SELECT a FROM AuditLog a ORDER BY a.performedAt DESC")
    List<AuditLog> findTopNByOrderByPerformedAtDesc(PageRequest pageRequest);

    default List<AuditLog> findTopNByOrderByPerformedAtDesc(int limit) {
        return findTopNByOrderByPerformedAtDesc(PageRequest.of(0, limit));
    }

    long countByEntityType(AuditEntityType entityType);

    long countByAction(AuditAction action);

    long countByPerformedAtAfter(LocalDateTime dateTime);
}