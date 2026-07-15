package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.response.BookingStatsByPeriodResponse;
import com.petbuddy.petbuddystore.dto.response.BookingStatsByServiceResponse;
import com.petbuddy.petbuddystore.dto.response.BookingStatsSummaryResponse;
import com.petbuddy.petbuddystore.service.BookingStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics/bookings")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Booking Statistics API", description = "Thống kê doanh thu và đơn đặt lịch dịch vụ")
public class BookingStatisticsController {

    BookingStatisticsService bookingStatisticsService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê tổng kết (KPIs)",
            description = "Trả về tổng doanh thu, số đơn hoàn thành, hủy, đang xử lý và giá trị đơn hàng trung bình theo khoảng thời gian."
    )
    public ResponseEntity<ApiResponse<BookingStatsSummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(bookingStatisticsService.getSummary(from, to)));
    }

    @GetMapping("/by-service")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê theo dịch vụ (Biểu đồ tròn - Pie Chart)",
            description = "Trả về doanh thu và số lượng đơn của từng loại dịch vụ, kèm tỷ lệ phần trăm."
    )
    public ResponseEntity<ApiResponse<List<BookingStatsByServiceResponse>>> getStatsByService(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(bookingStatisticsService.getStatsByService(from, to)));
    }

    @GetMapping("/by-period")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Thống kê theo thời gian (Biểu đồ cột/đường - Bar/Line Chart)",
            description = "Trả về doanh thu và số lượng đơn theo từng ngày/tuần/tháng. " +
                    "Tham số groupBy nhận giá trị: DAY (mặc định), WEEK, MONTH."
    )
    public ResponseEntity<ApiResponse<List<BookingStatsByPeriodResponse>>> getStatsByPeriod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false, defaultValue = "DAY") String groupBy
    ) {
        return ResponseEntity.ok(ApiResponse.success(bookingStatisticsService.getStatsByPeriod(from, to, groupBy)));
    }
}
