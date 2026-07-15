package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import com.petbuddy.petbuddystore.dto.response.BookingStatsByPeriodResponse;
import com.petbuddy.petbuddystore.dto.response.BookingStatsByServiceResponse;
import com.petbuddy.petbuddystore.dto.response.BookingStatsSummaryResponse;
import com.petbuddy.petbuddystore.model.Booking;
import com.petbuddy.petbuddystore.model.BookingDetail;
import com.petbuddy.petbuddystore.repository.BookingRepository;
import com.petbuddy.petbuddystore.service.BookingStatisticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingStatisticsServiceImpl implements BookingStatisticsService {

    BookingRepository bookingRepository;

    // ── Lấy danh sách đơn trong khoảng thời gian (chỉ theo scheduledAt)
    private List<Booking> fetchBookingsInRange(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = (from != null ? from : LocalDate.of(1970, 1, 1)).atStartOfDay();
        LocalDateTime toDt   = (to   != null ? to   : LocalDate.of(9999, 12, 31)).atTime(LocalTime.MAX);
        Collection<BookingStatus> allStatuses = Arrays.asList(BookingStatus.values());
        return bookingRepository.findByBookingStatusInAndScheduledAtBetweenOrderByCreateAtDesc(allStatuses, fromDt, toDt);
    }

    // ─────────────────────────────────────────────────────────────
    // API 1: TỔNG KẾT (Summary KPIs)
    // ─────────────────────────────────────────────────────────────
    @Override
    public BookingStatsSummaryResponse getSummary(LocalDate from, LocalDate to) {
        List<Booking> bookings = fetchBookingsInRange(from, to);

        long total      = bookings.size();
        long completed  = bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED).count();
        long cancelled  = bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count();
        long pending    = total - completed - cancelled
                - bookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.FAILED).count();

        // Chỉ tính doanh thu từ đơn COMPLETED
        BigDecimal totalRevenue = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avg = completed > 0
                ? totalRevenue.divide(BigDecimal.valueOf(completed), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return BookingStatsSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(total)
                .completedBookings(completed)
                .cancelledBookings(cancelled)
                .pendingBookings(pending)
                .averageOrderValue(avg)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // API 2: THEO DỊCH VỤ (Pie Chart)
    // ─────────────────────────────────────────────────────────────
    @Override
    public List<BookingStatsByServiceResponse> getStatsByService(LocalDate from, LocalDate to) {
        List<Booking> bookings = fetchBookingsInRange(from, to).stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED)
                .toList();

        // Gom nhóm theo catalogName (lấy từ BookingDetail snapshot)
        Map<String, List<BookingDetail>> grouped = new LinkedHashMap<>();
        for (Booking booking : bookings) {
            for (BookingDetail detail : booking.getBookingDetails()) {
                String name = detail.getCatalogName() != null ? detail.getCatalogName() : "Không xác định";
                grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(detail);
            }
        }

        BigDecimal grandTotal = grouped.values().stream()
                .flatMap(List::stream)
                .map(d -> d.getTotalPrice() != null ? d.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal revenue = entry.getValue().stream()
                            .map(d -> d.getTotalPrice() != null ? d.getTotalPrice() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    double percentage = grandTotal.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                            : revenue.divide(grandTotal, 4, RoundingMode.HALF_UP)
                                     .multiply(BigDecimal.valueOf(100))
                                     .doubleValue();

                    return BookingStatsByServiceResponse.builder()
                            .serviceName(entry.getKey())
                            .bookingCount(entry.getValue().size())
                            .revenue(revenue)
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(BookingStatsByServiceResponse::getRevenue).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // API 3: THEO THỜI GIAN (Bar / Line Chart)
    // ─────────────────────────────────────────────────────────────
    @Override
    public List<BookingStatsByPeriodResponse> getStatsByPeriod(LocalDate from, LocalDate to, String groupBy) {
        List<Booking> bookings = fetchBookingsInRange(from, to).stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED)
                .toList();

        // Hàm tạo key theo nhóm: DAY, WEEK, MONTH
        String pattern = switch (groupBy != null ? groupBy.toUpperCase() : "DAY") {
            case "MONTH" -> "yyyy-MM";
            case "WEEK"  -> "yyyy-'W'ww";
            default      -> "yyyy-MM-dd";
        };
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern, Locale.getDefault());

        Map<String, List<Booking>> grouped = bookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getScheduledAt().format(fmt),
                        TreeMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal revenue = entry.getValue().stream()
                            .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return BookingStatsByPeriodResponse.builder()
                            .period(entry.getKey())
                            .revenue(revenue)
                            .bookingCount(entry.getValue().size())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
