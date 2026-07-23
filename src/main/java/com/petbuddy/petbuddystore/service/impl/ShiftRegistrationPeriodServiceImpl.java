package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.RegistrationPeriodStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.RegistrationPeriodCreationRequest;
import com.petbuddy.petbuddystore.dto.response.RegistrationPeriodResponse;
import com.petbuddy.petbuddystore.mapper.ShiftRegistrationPeriodMapper;
import com.petbuddy.petbuddystore.model.ShiftRegistrationPeriod;
import com.petbuddy.petbuddystore.repository.ShiftRegistrationPeriodRepository;
import com.petbuddy.petbuddystore.service.ShiftRegistrationPeriodService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShiftRegistrationPeriodServiceImpl implements ShiftRegistrationPeriodService {
    ShiftRegistrationPeriodRepository shiftRegistrationPeriodRepository;
    ShiftRegistrationPeriodMapper shiftRegistrationPeriodMapper;

    @Override
    public RegistrationPeriodResponse createShiftRegistration(RegistrationPeriodCreationRequest request) {
        if (request.getWorkFromDate().isAfter(request.getWorkToDate())) {
            throw new AppException(ErrorCode.INVALID_WORKING_TIME);
        }

        if (!request.getRegisterOpenAt().isBefore(request.getRegisterCloseAt())) {
            throw new AppException(ErrorCode.INVALID_REGISTER_TIME);
        }

        if (request.getRegisterCloseAt().toLocalDate().isAfter(request.getWorkFromDate())) {
            throw new AppException(ErrorCode.INVALID_REGISTER_CLOSE_TIME);
        }

        boolean existed = shiftRegistrationPeriodRepository.existsByWorkFromDateLessThanEqualAndWorkToDateGreaterThanEqual(
                        request.getWorkToDate(), request.getWorkFromDate());

        if (existed) {
            throw new AppException(ErrorCode.REGISTRATION_PERIOD_ALREADY_EXISTS);
        }

        ShiftRegistrationPeriod period = shiftRegistrationPeriodMapper.toShiftRegistrationPeriod(request);
        period.setStatus(RegistrationPeriodStatus.OPEN);

        return shiftRegistrationPeriodMapper.toRegistrationPeriodResponse(shiftRegistrationPeriodRepository.save(period));
    }

    @Override
    public RegistrationPeriodResponse updateStatus(String periodId, RegistrationPeriodStatus status) {
        ShiftRegistrationPeriod period = shiftRegistrationPeriodRepository.findById(periodId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_PERIOD_NOT_EXISTED));

        period.setStatus(status);
        return shiftRegistrationPeriodMapper.toRegistrationPeriodResponse(shiftRegistrationPeriodRepository.save(period));
    }

    @Override
    public Page<RegistrationPeriodResponse> getRegistrationPeriods(LocalDate fromDate, LocalDate toDate, RegistrationPeriodStatus status, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ShiftRegistrationPeriod> registrationPeriodPage = shiftRegistrationPeriodRepository
                .findRegistrationPeriods(fromDate, toDate, status, pageable);
        return registrationPeriodPage.map(shiftRegistrationPeriodMapper::toRegistrationPeriodResponse);
    }

    @Override
    public RegistrationPeriodResponse getRegistrationById(String periodId) {
        ShiftRegistrationPeriod period = shiftRegistrationPeriodRepository.findById(periodId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_PERIOD_NOT_EXISTED));
        return shiftRegistrationPeriodMapper.toRegistrationPeriodResponse(period);
    }
}
