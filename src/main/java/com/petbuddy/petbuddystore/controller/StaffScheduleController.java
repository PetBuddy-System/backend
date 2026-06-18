package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.response.StaffScheduleResponse;
import com.petbuddy.petbuddystore.dto.response.WorkScheduleResponse;
import com.petbuddy.petbuddystore.service.StaffScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff-schedules")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Staff Schedule API", description = "Quản lí lịch làm việc của Staff (Staff tự quản lý)")
public class StaffScheduleController {
    StaffScheduleService staffScheduleService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<StaffScheduleResponse>>> getMySchedules(@RequestParam(required = false) LocalDate fromDate,
                                                                                   @RequestParam(required = false) LocalDate toDate,
                                                                                   @RequestParam(required = false) ScheduleStatus scheduleStatus) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(staffScheduleService.getMySchedules(fromDate, toDate, scheduleStatus)));
    }

    @PatchMapping("/{staffScheduleId}/check-in")
    public ResponseEntity<ApiResponse<StaffScheduleResponse>> checkIn(@PathVariable String staffScheduleId) {
        return ResponseEntity.ok(ApiResponse.success(staffScheduleService.checkIn(staffScheduleId)));
    }

    @PatchMapping("/{staffScheduleId}/check-out")
    public ResponseEntity<ApiResponse<StaffScheduleResponse>> checkOut(@PathVariable String staffScheduleId) {
        return ResponseEntity.ok(ApiResponse.success(staffScheduleService.checkOut(staffScheduleId)));
    }

}
