package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ShiftRegistrationResponse;

import java.util.List;

public interface StaffShiftRegistrationService {
    List<ShiftRegistrationResponse> registerShift(ShiftRegistrationCreationRequest request);
    List<ShiftRegistrationResponse> getMyRegistrations(String registrationPeriodId);
    List<ShiftRegistrationResponse> updateShiftRegistration(String registrationPeriodId, ShiftRegistrationUpdateRequest request);
}
