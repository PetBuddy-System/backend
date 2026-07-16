package com.petbuddy.petbuddystore.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardResponse {

    StatCard totalRevenue;
    StatCard profit;
    StatCard avgOrderValue;

    List<TrendPoint> revenueTrend;
    List<CompositionItem> revenueComposition;

    @Builder
    public record StatCard(
            BigDecimal value,
            Double changePercent
    ) {}

    @Builder
    public record TrendPoint(
            LocalDate date,
            String label,
            BigDecimal revenue
    ) {}

    @Builder
    public record CompositionItem(
            String label,
            BigDecimal amount,
            Double percent
    ) {}
}