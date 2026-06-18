package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.StaffReassignRequest;
import com.petbuddy.petbuddystore.dto.request.StaffsAssignRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleCreationRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.StaffScheduleResponse;
import com.petbuddy.petbuddystore.dto.response.WorkScheduleResponse;
import com.petbuddy.petbuddystore.mapper.StaffScheduleMapper;
import com.petbuddy.petbuddystore.mapper.WorkScheduleMapper;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.model.WorkSchedule;
import com.petbuddy.petbuddystore.repository.StaffScheduleRepository;
import com.petbuddy.petbuddystore.repository.WorkScheduleRepository;
import com.petbuddy.petbuddystore.repository.UserRepository;
import com.petbuddy.petbuddystore.service.WorkScheduleService;
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
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WorkScheduleServiceImpl implements WorkScheduleService {
    WorkScheduleMapper workScheduleMapper;
    WorkScheduleRepository workScheduleRepository;
    UserRepository userRepository;
    StaffScheduleRepository staffScheduleRepository;
    StaffScheduleMapper staffScheduleMapper;

    @Override
    public WorkScheduleResponse createWorkSchedule(WorkScheduleCreationRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new AppException(ErrorCode.INVALID_WORKING_TIME);
        }

        if (workScheduleRepository.existsByWorkDateAndStartTimeAndEndTime(request.getWorkDate(),
                request.getStartTime(), request.getEndTime())) {
            throw new AppException(ErrorCode.WORK_SCHEDULE_EXISTED);
        }

        WorkSchedule workSchedule = workScheduleMapper.toWorkSchedule(request);
        workSchedule = workScheduleRepository.save(workSchedule);

        if (request.getStaffIds() != null && !request.getStaffIds().isEmpty()){
            assignStaffToSchedule(workSchedule, request.getStaffIds());
        }

        WorkSchedule savedWorkSchedule = workScheduleRepository.findByIdWithStaffs(workSchedule.getWorkScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_EXISTED));

        return workScheduleMapper.toWorkScheduleResponse(savedWorkSchedule);
    }

    @Override
    public WorkScheduleResponse getWorkScheduleById(String workScheduleId) {
        WorkSchedule workSchedule = workScheduleRepository.findByIdWithStaffs(workScheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_EXISTED));
        return workScheduleMapper.toWorkScheduleResponse(workSchedule);
    }

    @Override
    public Page<WorkScheduleResponse> getWorkSchedules(LocalDate fromDate, LocalDate toDate, ShiftType shiftType,
                                                       int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<WorkSchedule> workSchedulePage = workScheduleRepository.findWorkSchedules(fromDate, toDate, shiftType, pageable);
        return workSchedulePage.map(workScheduleMapper::toWorkScheduleResponse);
    }

    @Override
    public WorkScheduleResponse updateWorkSchedule(String workScheduleId, WorkScheduleUpdateRequest request) {
        WorkSchedule workSchedule = workScheduleRepository.findByIdWithStaffs(workScheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_EXISTED));

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new AppException(ErrorCode.INVALID_WORKING_TIME);
        }

        boolean hasStarted = staffScheduleRepository.existsByWorkScheduleIdAndStatusIn(workScheduleId,
                List.of(ScheduleStatus.WORKING, ScheduleStatus.COMPLETED));

        if (hasStarted) {
            throw new AppException(ErrorCode.CANNOT_UPDATE);
        }

        for (StaffSchedule staffSchedule : workSchedule.getStaffSchedules()) {
            String staffId = staffSchedule.getStaff().getUserId();

            boolean hasExistedSchedule = staffScheduleRepository.existsSchedule(
                    staffId, request.getWorkDate(), request.getStartTime(),
                    request.getEndTime(), workScheduleId, List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.WORKING));

            if (hasExistedSchedule) {
                throw new AppException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
            }
        }

        workScheduleMapper.updateWorkSchedule(workSchedule, request);
        WorkSchedule savedWorkSchedule = workScheduleRepository.save(workSchedule);

        WorkSchedule result = workScheduleRepository.findByIdWithStaffs(savedWorkSchedule.getWorkScheduleId())
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_EXISTED));
        return workScheduleMapper.toWorkScheduleResponse(result);
    }

    @Override
    public WorkScheduleResponse assignStaffsToWorkSchedule(String workScheduleId, StaffsAssignRequest request) {
        WorkSchedule workSchedule = workScheduleRepository.findByIdWithStaffs(workScheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_EXISTED));

        if (request.getStaffIds() == null || request.getStaffIds().isEmpty()) {
            throw new AppException(ErrorCode.STAFF_LIST_EMPTY);
        }

        assignStaffToSchedule(workSchedule, request.getStaffIds());
        return workScheduleMapper.toWorkScheduleResponse(workSchedule);
    }

    @Override
    public void removeStaffFromWorkSchedule(String staffScheduleId) {
        StaffSchedule staffSchedule = staffScheduleRepository.findById(staffScheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_SCHEDULE_NOT_EXISTED));

        if (staffSchedule.getScheduleStatus() != ScheduleStatus.SCHEDULED) {
            throw new AppException(ErrorCode.CANNOT_UPDATE);
        }

        staffSchedule.setScheduleStatus(ScheduleStatus.CANCELLED);
        staffScheduleRepository.save(staffSchedule);
    }

    @Override
    public StaffScheduleResponse reassignStaffToWorkSchedule(String staffScheduleId, StaffReassignRequest request) {
        StaffSchedule staffSchedule = staffScheduleRepository.findById(staffScheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_SCHEDULE_NOT_EXISTED));

        if (staffSchedule.getScheduleStatus() != ScheduleStatus.SCHEDULED) {
            throw new AppException(ErrorCode.CANNOT_UPDATE);
        }

        User newStaff = validateStaff(request.getNewStaff());
        User oldStaff = staffSchedule.getStaff();

        if (oldStaff.getUserId().equals(newStaff.getUserId())) {
            throw new AppException(ErrorCode.SAME_STAFF);
        }

        WorkSchedule workSchedule = staffSchedule.getWorkSchedule();
        validateStaffAssignedToSchedule(newStaff.getUserId(), workSchedule.getWorkScheduleId(), workSchedule.getWorkDate(),
                workSchedule.getStartTime(), workSchedule.getEndTime());

        staffSchedule.setStaff(newStaff);
        return staffScheduleMapper.toStaffScheduleResponse(staffScheduleRepository.save(staffSchedule));
    }

    private void assignStaffToSchedule(WorkSchedule workSchedule, List<String> staffIds) {
        for (String staffId : staffIds) {
            User staff = validateStaff(staffId);
            validateStaffAssignedToSchedule(staffId, workSchedule.getWorkScheduleId(), workSchedule.getWorkDate(),
                    workSchedule.getStartTime(), workSchedule.getEndTime());

            StaffSchedule staffSchedule = StaffSchedule.builder()
                    .staff(staff)
                    .workSchedule(workSchedule)
                    .assignedAt(LocalDateTime.now())
                    .scheduleStatus(ScheduleStatus.SCHEDULED)
                    .build();
            StaffSchedule savedStaffSchedule = staffScheduleRepository.save(staffSchedule);
            workSchedule.getStaffSchedules().add(savedStaffSchedule);
        }
    }

    private void validateStaffAssignedToSchedule(String staffId, String workScheduleId, LocalDate workDate,
                                                 LocalTime startTime, LocalTime endTime){
        boolean scheduleAssigned = staffScheduleRepository.existsByStaff_UserIdAndWorkSchedule_WorkScheduleId
                (staffId, workScheduleId);

        if (scheduleAssigned) {
            throw new AppException(ErrorCode.STAFF_ALREADY_ASSIGNED_TO_SCHEDULE);
        }

        boolean hasExistedSchedule = staffScheduleRepository.existsSchedule(
                staffId, workDate, startTime, endTime, workScheduleId, List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.WORKING));

        if (hasExistedSchedule) {
            throw new AppException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
        }
    }

    private User validateStaff(String staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (staff.getRole() != Role.STAFF) {
            throw new AppException(ErrorCode.USER_NOT_STAFF);
        }
        return staff;
    }
}
