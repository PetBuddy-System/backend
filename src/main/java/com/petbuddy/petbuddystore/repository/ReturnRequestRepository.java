package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ReturnStatus;
import com.petbuddy.petbuddystore.model.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long>, JpaSpecificationExecutor<ReturnRequest> {

    Page<ReturnRequest> findByRequestedBy_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    boolean existsByOrder_OrderIdAndStatusIn(Long orderId, Collection<ReturnStatus> statuses);

    boolean existsByReturnCode(String returnCode);

    @Query("SELECT rr FROM ReturnRequest rr " +
            "JOIN FETCH rr.returnItems ri " +
            "JOIN FETCH ri.orderDetail od " +
            "WHERE rr.returnRequestId = :returnRequestId")
    Optional<ReturnRequest> findByIdWithItems(@Param("returnRequestId") Long returnRequestId);
}

