package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.response.StaffScheduleResponse;
import com.petbuddy.petbuddystore.mapper.StaffScheduleMapper;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.StaffScheduleRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.StaffScheduleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StaffScheduleServiceImpl implements StaffScheduleService {
    StaffScheduleMapper staffScheduleMapper;
    StaffScheduleRepository staffScheduleRepository;
    UserRepository userRepository;

    @Override
    public List<StaffScheduleResponse> getMySchedules(LocalDate fromDate, LocalDate toDate, ScheduleStatus scheduleStatus) {
        User staff = getCurrentStaff();

        List<StaffSchedule> staffSchedules = staffScheduleRepository.findMySchedules(staff.getUserId(), fromDate, toDate, scheduleStatus);
        return staffSchedules.stream()
                .map(staffScheduleMapper::toStaffScheduleResponse)
                .toList();
    }

    @Override
    public StaffScheduleResponse checkIn(String staffScheduleId) {
        User staff = getCurrentStaff();
        StaffSchedule staffSchedule = staffScheduleRepository.findByIdWithWorkScheduleAndStaff(staffScheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_SCHEDULE_NOT_EXISTED));
        validateOwner(staffSchedule, staff.getUserId());

        if (staffSchedule.getScheduleStatus() != ScheduleStatus.SCHEDULED) {
            throw new AppException(ErrorCode.CANNOT_CHECKIN);
        }

        staffSchedule.setScheduleStatus(ScheduleStatus.WORKING);
        staffSchedule.setCheckInAt(LocalDateTime.now());
        return staffScheduleMapper.toStaffScheduleResponse(staffScheduleRepository.save(staffSchedule));

    }

    @Override
    public StaffScheduleResponse checkOut(String staffScheduleId) {
        User staff = getCurrentStaff();
        StaffSchedule staffSchedule = staffScheduleRepository.findByIdWithWorkScheduleAndStaff(staffScheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_SCHEDULE_NOT_EXISTED));
        validateOwner(staffSchedule, staff.getUserId());

        if (staffSchedule.getScheduleStatus() != ScheduleStatus.WORKING) {
            throw new AppException(ErrorCode.CANNOT_CHECKOUT);
        }

        staffSchedule.setScheduleStatus(ScheduleStatus.COMPLETED);
        staffSchedule.setCheckOutAt(LocalDateTime.now());
        return staffScheduleMapper.toStaffScheduleResponse(staffScheduleRepository.save(staffSchedule));
    }

    private User getCurrentStaff() {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return validateStaff(currentUserId);
    }

    private User validateStaff(String staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (staff.getRole() != Role.STAFF) {
            throw new AppException(ErrorCode.USER_NOT_STAFF);
        }
        return staff;
    }

    private void validateOwner(StaffSchedule staffSchedule, String userId) {
        if (!staffSchedule.getStaff().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}
