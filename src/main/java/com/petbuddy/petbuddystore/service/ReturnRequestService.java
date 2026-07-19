package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.CalculateRefundRequest;
import com.petbuddy.petbuddystore.dto.request.CreateReturnRequest;
import com.petbuddy.petbuddystore.dto.request.ReturnFilterRequest;
import com.petbuddy.petbuddystore.dto.request.UpdateReturnStatusRequest;
import com.petbuddy.petbuddystore.dto.response.CalculateRefundResponse;
import com.petbuddy.petbuddystore.dto.response.ReturnRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReturnRequestService {
    CalculateRefundResponse calculateRefund(CalculateRefundRequest request);

    ReturnRequestResponse createReturnRequest(CreateReturnRequest request);

    ReturnRequestResponse uploadMedia(Long id, List<MultipartFile> files);

    Page<ReturnRequestResponse> getMyReturnRequests(Pageable pageable);

    ReturnRequestResponse getReturnRequestById(Long id);

    ReturnRequestResponse cancelReturnRequest(Long id);

    Page<ReturnRequestResponse> getAllReturnRequests(ReturnFilterRequest filter, String sortBy, Pageable pageable);

    ReturnRequestResponse updateReturnStatusByManagement(Long id, UpdateReturnStatusRequest request);

    ReturnRequestResponse updateReturnStatusByShipper(Long id, UpdateReturnStatusRequest request);
}
