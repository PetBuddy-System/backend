package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.ShiftType;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ShiftRegistrationUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ShiftRegistrationResponse;
import com.petbuddy.petbuddystore.service.StaffShiftRegistrationService;
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
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shift/registrations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "StaffShiftRegistration API", description = "Quản lý staff đăng ký ca làm việc")
public class StaffShiftRegistrationController {
    StaffShiftRegistrationService staffShiftRegistrationService;

    @PreAuthorize("hasRole('STAFF')")
    @PostMapping()
    @Operation(description = "Staff đăng ký ca làm việc")
    public ResponseEntity<ApiResponse<List<ShiftRegistrationResponse>>> registerShift(@RequestBody @Valid ShiftRegistrationCreationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shift registered successfully", staffShiftRegistrationService.registerShift(request)));
    }

    @PreAuthorize("hasRole('STAFF')")
    @GetMapping("/me/{registrationPeriodId}")
    @Operation(description = "Staff xem đăng ký ca của mình trong một registration period")
    public ResponseEntity<ApiResponse<List<ShiftRegistrationResponse>>> getMyRegistrations(@PathVariable String registrationPeriodId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(staffShiftRegistrationService.getMyRegistrations(registrationPeriodId)));
    }

    @PreAuthorize("hasRole('STAFF')")
    @PutMapping("/me/{registrationPeriodId}")
    @Operation(description = "Staff cập nhật đăng ký ca của mình nếu còn trong thời gian đăng ký")
    public ResponseEntity<ApiResponse<List<ShiftRegistrationResponse>>> updateShiftRegistration(@PathVariable String registrationPeriodId,
                                                                                                @RequestBody @Valid ShiftRegistrationUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Shift registration updated successfully", staffShiftRegistrationService.updateShiftRegistration(registrationPeriodId, request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/period/{registrationPeriodId}")
    @Operation(description = "Manager xem toàn bộ staff đăng ký ca trong một registration period, có phân trang và filter")
    public ResponseEntity<ApiResponse<Page<ShiftRegistrationResponse>>> getRegistrationsForManager(@PathVariable String registrationPeriodId,
                                                                                                   @RequestParam(required = false) String staffKeyword,
                                                                                                   @RequestParam(required = false) LocalDate workDate,
                                                                                                   @RequestParam(required = false) ShiftType shiftType,
                                                                                                   @RequestParam(defaultValue = "0") int page,
                                                                                                   @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(
                        staffShiftRegistrationService.getRegistrationsForManager(registrationPeriodId, staffKeyword, workDate,
                                shiftType, page, size)));
    }
}
