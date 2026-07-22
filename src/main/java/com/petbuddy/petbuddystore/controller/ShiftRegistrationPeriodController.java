package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.RegistrationPeriodStatus;
import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.RegistrationPeriodCreationRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleCreationRequest;
import com.petbuddy.petbuddystore.dto.request.WorkScheduleUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.RegistrationPeriodResponse;
import com.petbuddy.petbuddystore.dto.response.WorkScheduleResponse;
import com.petbuddy.petbuddystore.service.ShiftRegistrationPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shift/registration-period")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "ShiftRegistrationPeriod API", description = "Quản lý đăng ký ca làm việc")
public class ShiftRegistrationPeriodController {
    ShiftRegistrationPeriodService shiftRegistrationPeriodService;

    @PostMapping()
    @Operation(description = "Tạo mới đăng ký ca làm việc")
    public ResponseEntity<ApiResponse<RegistrationPeriodResponse>> createShiftRegistration(@RequestBody @Valid RegistrationPeriodCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shift registration created successfully", shiftRegistrationPeriodService.createShiftRegistration(request)));
    }

    @GetMapping()
    @Operation(description = "Lấy tất cả đăng ký lịch làm việc có phân trang, filter theo date, theo status")
    public ResponseEntity<ApiResponse<Page<RegistrationPeriodResponse>>> getRegistrationPeriods(@RequestParam(required = false) LocalDate fromDate,
                                                                                    @RequestParam(required = false) LocalDate toDate,
                                                                                    @RequestParam(required = false) RegistrationPeriodStatus status,
                                                                                    @RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(shiftRegistrationPeriodService.getRegistrationPeriods(fromDate, toDate, status, page, size)));
    }

    @GetMapping("/{periodId}")
    @Operation(description = "Lấy thông tin đăng ký lịch làm việc theo id")
    public ResponseEntity<ApiResponse<RegistrationPeriodResponse>> getRegistrationById(@PathVariable String periodId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(shiftRegistrationPeriodService.getRegistrationById(periodId)));
    }

    @PutMapping("/{periodId}")
    @Operation(description = "Update status dang ky lịch làm việc theo id")
    public ResponseEntity<ApiResponse<RegistrationPeriodResponse>> updateStatus(@PathVariable String periodId, @RequestParam RegistrationPeriodStatus status){
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Registration updated successfully",shiftRegistrationPeriodService.updateStatus(periodId, status)));
    }
}
