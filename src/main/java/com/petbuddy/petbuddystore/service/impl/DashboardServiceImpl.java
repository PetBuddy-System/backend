package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.PaymentStatus;
import com.petbuddy.petbuddystore.common.enums.PeriodType;
import com.petbuddy.petbuddystore.dto.response.DashboardResponse;
import com.petbuddy.petbuddystore.model.Payment;
import com.petbuddy.petbuddystore.repository.OrderRepository;
import com.petbuddy.petbuddystore.repository.PaymentRepository;
import com.petbuddy.petbuddystore.service.DashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class DashboardServiceImpl implements DashboardService {

    OrderRepository orderRepository;
    PaymentRepository paymentRepository;

    @Override
    public DashboardResponse getRevenueDashboard(PeriodType periodType, LocalDate referenceDate) {
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        PeriodType type = periodType != null ? periodType : PeriodType.WEEK;

        LocalDate[] currentRange = resolveRange(type, refDate);
        LocalDate[] previousRange = resolvePreviousRange(type, currentRange[0]);

        LocalDateTime currentStart = currentRange[0].atStartOfDay();
        LocalDateTime currentEnd = currentRange[1].atTime(LocalTime.MAX);
        LocalDateTime previousStart = previousRange[0].atStartOfDay();
        LocalDateTime previousEnd = previousRange[1].atTime(LocalTime.MAX);

        List<Payment> currentPayments = paymentRepository
                .findByStatusAndOrderIsNotNullAndPaidAtBetween(PaymentStatus.PAID, currentStart, currentEnd);
        List<Payment> previousPayments = paymentRepository
                .findByStatusAndOrderIsNotNullAndPaidAtBetween(PaymentStatus.PAID, previousStart, previousEnd);

        BigDecimal currentRevenue = sumAmount(currentPayments);
        BigDecimal previousRevenue = sumAmount(previousPayments);

        long currentOrderCount = orderRepository.countByCreatedAtBetween(currentStart, currentEnd);
        long previousOrderCount = orderRepository.countByCreatedAtBetween(previousStart, previousEnd);

        BigDecimal currentAvgOrder = calculateAvg(currentRevenue, currentOrderCount);
        BigDecimal previousAvgOrder = calculateAvg(previousRevenue, previousOrderCount);

        DashboardResponse.StatCard totalRevenueCard = DashboardResponse.StatCard.builder()
                .value(currentRevenue)
                .changePercent(percentChange(currentRevenue, previousRevenue))
                .build();

        DashboardResponse.StatCard orderCountCard = DashboardResponse.StatCard.builder()
                .value(BigDecimal.valueOf(currentOrderCount))
                .changePercent(percentChange(BigDecimal.valueOf(currentOrderCount), BigDecimal.valueOf(previousOrderCount)))
                .build();

        DashboardResponse.StatCard avgOrderCard = DashboardResponse.StatCard.builder()
                .value(currentAvgOrder)
                .changePercent(percentChange(currentAvgOrder, previousAvgOrder))
                .build();

        List<DashboardResponse.TrendPoint> trend = buildTrend(currentRange[0], currentRange[1], currentPayments, type);

        List<DashboardResponse.CompositionItem> composition = new ArrayList<>();
        composition.add(DashboardResponse.CompositionItem.builder()
                .label("Sản phẩm")
                .amount(currentRevenue)
                .percent(currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0)
                .build());

        return DashboardResponse.builder()
                .totalRevenue(totalRevenueCard)
                .orderCount(orderCountCard)
                .avgOrderValue(avgOrderCard)
                .revenueTrend(trend)
                .revenueComposition(composition)
                .build();
    }

    private LocalDate[] resolveRange(PeriodType type, LocalDate refDate) {
        return switch (type) {
            case DAY -> new LocalDate[]{refDate, refDate};
            case WEEK -> {
                LocalDate monday = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new LocalDate[]{monday, monday.plusDays(6)};
            }
            case YEAR -> {
                LocalDate first = refDate.withDayOfYear(1);
                LocalDate last = refDate.withDayOfYear(refDate.lengthOfYear());
                yield new LocalDate[]{first, last};
            }
        };
    }

    private LocalDate[] resolvePreviousRange(PeriodType type, LocalDate currentStart) {
        return switch (type) {
            case DAY -> {
                LocalDate prev = currentStart.minusDays(1);
                yield new LocalDate[]{prev, prev};
            }
            case WEEK -> {
                LocalDate prevMonday = currentStart.minusWeeks(1);
                yield new LocalDate[]{prevMonday, prevMonday.plusDays(6)};
            }
            case YEAR -> {
                LocalDate prevYear = currentStart.minusYears(1);
                yield new LocalDate[]{
                        prevYear.withDayOfYear(1),
                        prevYear.withDayOfYear(prevYear.lengthOfYear())
                };
            }
        };
    }

    private List<DashboardResponse.TrendPoint> buildTrend(LocalDate start, LocalDate end,
                                                          List<Payment> payments, PeriodType type) {
        if (type == PeriodType.YEAR) {
            return buildMonthlyTrend(start.getYear(), payments);
        }
        return buildDailyTrend(start, end, payments);
    }

    private List<DashboardResponse.TrendPoint> buildDailyTrend(LocalDate start, LocalDate end,
                                                              List<Payment> payments) {
        TreeMap<LocalDate, BigDecimal> byDay = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            byDay.put(d, BigDecimal.ZERO);
        }
        for (Payment p : payments) {
            byDay.merge(p.getPaidAt().toLocalDate(), p.getAmount(), BigDecimal::add);
        }

        List<DashboardResponse.TrendPoint> result = new ArrayList<>();
        for (var entry : byDay.entrySet()) {
            String label = entry.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("vi"));
            result.add(DashboardResponse.TrendPoint.builder()
                    .date(entry.getKey())
                    .label(label)
                    .revenue(entry.getValue())
                    .build());
        }
        return result;
    }

    private List<DashboardResponse.TrendPoint> buildMonthlyTrend(int year, List<Payment> payments) {
        TreeMap<Integer, BigDecimal> byMonth = new TreeMap<>();
        for (int m = 1; m <= 12; m++) {
            byMonth.put(m, BigDecimal.ZERO);
        }
        for (Payment p : payments) {
            byMonth.merge(p.getPaidAt().getMonthValue(), p.getAmount(), BigDecimal::add);
        }

        List<DashboardResponse.TrendPoint> result = new ArrayList<>();
        for (var entry : byMonth.entrySet()) {
            LocalDate monthDate = LocalDate.of(year, entry.getKey(), 1);
            result.add(DashboardResponse.TrendPoint.builder()
                    .date(monthDate)
                    .label("Th " + entry.getKey())
                    .revenue(entry.getValue())
                    .build());
        }
        return result;
    }

    private BigDecimal sumAmount(List<Payment> payments) {
        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : payments) total = total.add(p.getAmount());
        return total;
    }

    private BigDecimal calculateAvg(BigDecimal revenue, long count) {
        if (count == 0) return BigDecimal.ZERO;
        return revenue.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
    }

    private Double percentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
