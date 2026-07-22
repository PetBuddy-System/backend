package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ShiftRegistrationResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface StaffShiftRegistrationService {
    List<ShiftRegistrationResponse> registerShift(ShiftRegistrationCreationRequest request);
    List<ShiftRegistrationResponse> getMyRegistrations(String registrationPeriodId);
    List<ShiftRegistrationResponse> updateShiftRegistration(String registrationPeriodId, ShiftRegistrationUpdateRequest request);
    Page<ShiftRegistrationResponse> getRegistrationsForManager(String registrationPeriodId,
            String staffKeyword, LocalDate workDate, ShiftType shiftType, int page, int size);
}
