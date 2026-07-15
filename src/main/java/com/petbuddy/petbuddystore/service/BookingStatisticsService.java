package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.response.BookingStatsByPeriodResponse;
import com.petbuddy.petbuddystore.dto.response.BookingStatsByServiceResponse;
import com.petbuddy.petbuddystore.dto.response.BookingStatsSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface BookingStatisticsService {
    BookingStatsSummaryResponse getSummary(LocalDate from, LocalDate to);
    List<BookingStatsByServiceResponse> getStatsByService(LocalDate from, LocalDate to);
    List<BookingStatsByPeriodResponse> getStatsByPeriod(LocalDate from, LocalDate to, String groupBy);
}
