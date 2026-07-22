package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.RegistrationPeriodStatus;
import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationItem;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ShiftRegistrationResponse;
import com.petbuddy.petbuddystore.mapper.StaffShiftRegistrationMapper;
import com.petbuddy.petbuddystore.model.ShiftRegistrationPeriod;
import com.petbuddy.petbuddystore.model.StaffShiftRegistration;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.ShiftRegistrationPeriodRepository;
import com.petbuddy.petbuddystore.repository.StaffShiftRegistrationRepository;
import com.petbuddy.petbuddystore.service.StaffShiftRegistrationService;
import com.petbuddy.petbuddystore.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StaffShiftRegistrationServiceImpl implements StaffShiftRegistrationService {
    StaffShiftRegistrationRepository staffShiftRegistrationRepository;
    ShiftRegistrationPeriodRepository shiftRegistrationPeriodRepository;
    StaffShiftRegistrationMapper staffShiftRegistrationMapper;
    UserService userService;

    @Override
    public List<ShiftRegistrationResponse> registerShift(ShiftRegistrationCreationRequest request) {
        User staff = getCurrentStaff();
        ShiftRegistrationPeriod period = shiftRegistrationPeriodRepository
                .findById(request.getRegistrationPeriodId())
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_PERIOD_NOT_EXISTED));

        validatePeriodCanRegister(period);

        if (request.getRegistrations() == null || request.getRegistrations().isEmpty()) {
            throw new AppException(ErrorCode.SHIFT_REGISTRATION_EMPTY);
        }

        boolean existed = staffShiftRegistrationRepository.existsMyRegistration(period.getRegistrationPeriodId(), staff.getUserId());
        if (existed) {
            throw new AppException(ErrorCode.SHIFT_REGISTRATION_ALREADY_EXISTS);
        }

        validateRegistrationItems(request.getRegistrations(), period);

        List<StaffShiftRegistration> registrations = request.getRegistrations()
                .stream()
                .map(item -> {
                    StaffShiftRegistration registration = staffShiftRegistrationMapper.toStaffShiftRegistration(item);
                    registration.setRegistrationPeriod(period);
                    registration.setStaff(staff);
                    return registration;
                })
                .toList();

        return staffShiftRegistrationRepository.saveAll(registrations)
                .stream()
                .map(staffShiftRegistrationMapper::toShiftRegistrationResponse)
                .toList();
    }

    @Override
    public List<ShiftRegistrationResponse> getMyRegistrations(String registrationPeriodId) {
        User staff = getCurrentStaff();

        if (!shiftRegistrationPeriodRepository.existsById(registrationPeriodId)) {
            throw new AppException(ErrorCode.REGISTRATION_PERIOD_NOT_EXISTED);
        }

        return staffShiftRegistrationRepository
                .findMyRegistrations(registrationPeriodId, staff.getUserId())
                .stream()
                .map(staffShiftRegistrationMapper::toShiftRegistrationResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<ShiftRegistrationResponse> updateShiftRegistration(String registrationPeriodId, ShiftRegistrationUpdateRequest request) {
        User staff = getCurrentStaff();

        ShiftRegistrationPeriod period = shiftRegistrationPeriodRepository.findById(registrationPeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTRATION_PERIOD_NOT_EXISTED));

        validatePeriodCanRegister(period);

        if (request.getRegistrations() == null || request.getRegistrations().isEmpty()) {
            throw new AppException(ErrorCode.SHIFT_REGISTRATION_EMPTY);
        }

        validateRegistrationItems(request.getRegistrations(), period);
        staffShiftRegistrationRepository.deleteMyRegistrations(registrationPeriodId, staff.getUserId());

        List<StaffShiftRegistration> registrations = request.getRegistrations()
                .stream()
                .map(item -> {
                    StaffShiftRegistration registration = staffShiftRegistrationMapper.toStaffShiftRegistration(item);
                    registration.setRegistrationPeriod(period);
                    registration.setStaff(staff);
                    return registration;
                })
                .toList();

        return staffShiftRegistrationRepository.saveAll(registrations)
                .stream()
                .map(staffShiftRegistrationMapper::toShiftRegistrationResponse)
                .toList();
    }

    @Override
    public Page<ShiftRegistrationResponse> getRegistrationsForManager(String registrationPeriodId, String staffKeyword, LocalDate workDate, ShiftType shiftType, int page, int size) {
        if (!shiftRegistrationPeriodRepository.existsById(registrationPeriodId)) {
            throw new AppException(ErrorCode.REGISTRATION_PERIOD_NOT_EXISTED);
        }

        Pageable pageable = PageRequest.of(page, size);

        String keyword = null;
        if (staffKeyword != null && !staffKeyword.isBlank()) {
            keyword = staffKeyword.trim();
        }

        return staffShiftRegistrationRepository.findRegistrationsForManager(registrationPeriodId, keyword, workDate, shiftType, pageable)
                .map(staffShiftRegistrationMapper::toShiftRegistrationResponse);
    }

    private void validatePeriodCanRegister(ShiftRegistrationPeriod period) {
        LocalDateTime now = LocalDateTime.now();

        if (period.getStatus() != RegistrationPeriodStatus.OPEN) {
            throw new AppException(ErrorCode.REGISTRATION_PERIOD_CLOSED);
        }

        if (now.isBefore(period.getRegisterOpenAt())) {
            throw new AppException(ErrorCode.REGISTRATION_NOT_OPEN);
        }

        if (!now.isBefore(period.getRegisterCloseAt())) {
            throw new AppException(ErrorCode.REGISTRATION_EXPIRED);
        }
    }

    private void validateRegistrationItems(List<ShiftRegistrationItem> items, ShiftRegistrationPeriod period) {
        Set<String> uniqueKeys = new HashSet<>();

        for (ShiftRegistrationItem item : items) {
            validateBasicRegistrationItem(item, period);

            if (item.getPreferredShift() == ShiftType.CUSTOM) {
                validateCustomShift(item);
            } else {
                validateNormalShift(item);
            }

            String key = buildDuplicateKey(item);

            if (!uniqueKeys.add(key)) {
                throw new AppException(ErrorCode.DUPLICATE_SHIFT_REGISTRATION);
            }
        }
    }

    private void validateBasicRegistrationItem(ShiftRegistrationItem item, ShiftRegistrationPeriod period) {
        if (item.getWorkDate() == null) {
            throw new AppException(ErrorCode.WORK_DATE_REQUIRED);
        }

        if (item.getPreferredShift() == null) {
            throw new AppException(ErrorCode.SHIFT_TYPE_REQUIRED);
        }

        if (item.getWorkDate().isBefore(period.getWorkFromDate())
                || item.getWorkDate().isAfter(period.getWorkToDate())) {
            throw new AppException(ErrorCode.WORK_DATE_OUT_OF_REGISTRATION_PERIOD);
        }
    }

    private void validateCustomShift(ShiftRegistrationItem item) {
        if (item.getPreferredStartTime() == null || item.getPreferredEndTime() == null) {
            throw new AppException(ErrorCode.CUSTOM_SHIFT_TIME_REQUIRED);
        }

        if (!item.getPreferredStartTime().isBefore(item.getPreferredEndTime())) {
            throw new AppException(ErrorCode.INVALID_SHIFT_TIME);
        }

        if (item.getReason() == null || item.getReason().isBlank()) {
            throw new AppException(ErrorCode.CUSTOM_SHIFT_REASON_REQUIRED);
        }
    }

    private void validateNormalShift(ShiftRegistrationItem item) {
        if (item.getPreferredStartTime() != null
                || item.getPreferredEndTime() != null
                || (item.getReason() != null && !item.getReason().isBlank())) {
            throw new AppException(ErrorCode.CUSTOM_SHIFT_FIELDS_NOT_ALLOWED);
        }
    }

    private String buildDuplicateKey(ShiftRegistrationItem item) {
        if (item.getPreferredShift() == ShiftType.CUSTOM) {
            return item.getWorkDate()
                    + "|"
                    + item.getPreferredShift()
                    + "|"
                    + item.getPreferredStartTime()
                    + "|"
                    + item.getPreferredEndTime();
        }

        return item.getWorkDate()
                + "|"
                + item.getPreferredShift();
    }

    private User getCurrentStaff() {
        return userService.getCurrentUserEntity();
    }

}
