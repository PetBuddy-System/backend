package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ReturnStatus;
import com.petbuddy.petbuddystore.model.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long>, JpaSpecificationExecutor<ReturnRequest> {

    Page<ReturnRequest> findByRequestedBy_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    boolean existsByOrder_OrderIdAndStatusIn(Long orderId, Collection<ReturnStatus> statuses);

    boolean existsByReturnCode(String returnCode);
}

