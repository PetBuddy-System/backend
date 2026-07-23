package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.PeriodType;
import com.petbuddy.petbuddystore.dto.response.DashboardResponse;

import java.time.LocalDate;

public interface DashboardService {
    DashboardResponse getRevenueDashboard(PeriodType periodType, LocalDate referenceDate);
}
