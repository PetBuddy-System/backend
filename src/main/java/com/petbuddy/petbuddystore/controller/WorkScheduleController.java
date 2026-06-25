package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.StaffReassignRequest;
import com.petbuddy.petbuddystore.dto.request.StaffsAssignRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleCreationRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.StaffScheduleResponse;
import com.petbuddy.petbuddystore.dto.response.WorkScheduleResponse;
import com.petbuddy.petbuddystore.service.WorkScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work-schedules")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Work Schedule API", description = "Quản lí lịch làm việc của Staff (CRUD SCHEDULE)")
public class WorkScheduleController {
    WorkScheduleService workScheduleService;

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping()
    @Operation(description = "Tạo mới lịch làm việc")
    public ResponseEntity<ApiResponse<WorkScheduleResponse>> createWorkSchedule(@RequestBody @Valid WorkScheduleCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Work schedule created successfully", workScheduleService.createWorkSchedule(request)));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/{workScheduleId}")
    @Operation(description = "Lấy thông tin chi tiết lịch làm việc theo id")
    public ResponseEntity<ApiResponse<WorkScheduleResponse>> getWorkScheduleById(@PathVariable String workScheduleId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(workScheduleService.getWorkScheduleById(workScheduleId)));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping()
    @Operation(description = "Lấy tất cả lịch làm việc có phân trang, filter theo date, theo shiftype")
    public ResponseEntity<ApiResponse<Page<WorkScheduleResponse>>> getWorkSchedules(@RequestParam(required = false) LocalDate fromDate,
                                                                                    @RequestParam(required = false) LocalDate toDate,
                                                                                    @RequestParam(required = false) ShiftType shiftType,
                                                                                    @RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(workScheduleService.getWorkSchedules(fromDate, toDate, shiftType, page, size)));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{workScheduleId}")
    @Operation(description = "Update thông tin lịch làm việc theo id")
    public ResponseEntity<ApiResponse<WorkScheduleResponse>> updateWorkSchedule(@PathVariable String workScheduleId,
                                                                                @RequestBody @Valid WorkScheduleUpdateRequest request){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Work schedule updated successfully",workScheduleService.updateWorkSchedule(workScheduleId, request)));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/{workScheduleId}/staff")
    @Operation(description = "Thêm các staff mới vào lịch làm việc đã có")
    public ResponseEntity<ApiResponse<WorkScheduleResponse>> assignStaffsToWorkSchedule(@PathVariable String workScheduleId,
                                                                                        @RequestBody StaffsAssignRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Staffs assigned successfully",workScheduleService.assignStaffsToWorkSchedule(workScheduleId, request)));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/staff-schedules/{staffScheduleId}/remove")
    @Operation(description = "Xóa mềm staff khỏi lịch làm với status = CANCELLED")
    public ResponseEntity<ApiResponse<Void>> removeStaffFromWorkSchedule(@PathVariable String staffScheduleId) {
        workScheduleService.removeStaffFromWorkSchedule(staffScheduleId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Staff removed successfully",null));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/staff-schedules/{staffScheduleId}/reassign")
    @Operation(description = "Thay staff mới cho staff cũ vào lịch làm việc")
    public ResponseEntity<ApiResponse<StaffScheduleResponse>> reassignStaffToWorkSchedule(@PathVariable String staffScheduleId,
                                                                          @RequestBody StaffReassignRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Staff reassigned successfully",workScheduleService.reassignStaffToWorkSchedule(staffScheduleId, request)));
    }
}
