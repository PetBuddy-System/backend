package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.PeriodType;
import com.petbuddy.petbuddystore.dto.response.DashboardResponse;
import com.petbuddy.petbuddystore.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Dashboard API", description = "API quản lý dashboard")
public class DashboardController {
      DashboardService dashboardService;

    @GetMapping("/revenue")
    public DashboardResponse getRevenueDashboard(
            @RequestParam(required = false) PeriodType periodType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate referenceDate) {
        return dashboardService.getRevenueDashboard(periodType, referenceDate);
    }
}
