package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.RegistrationPeriodStatus;
import com.petbuddy.petbuddystore.dto.request.RegistrationPeriodCreationRequest;
import com.petbuddy.petbuddystore.dto.response.RegistrationPeriodResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface ShiftRegistrationPeriodService {
    RegistrationPeriodResponse createShiftRegistration(RegistrationPeriodCreationRequest request);
    RegistrationPeriodResponse updateStatus(String periodId, RegistrationPeriodStatus status);
    Page<RegistrationPeriodResponse> getRegistrationPeriods(LocalDate fromDate, LocalDate toDate,
                                                            RegistrationPeriodStatus status, int page, int size);
    RegistrationPeriodResponse getRegistrationById(String periodId);
}
