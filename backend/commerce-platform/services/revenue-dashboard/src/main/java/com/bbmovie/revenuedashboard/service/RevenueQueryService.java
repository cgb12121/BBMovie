package com.bbmovie.revenuedashboard.service;

import com.bbmovie.revenuedashboard.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "revenue.analytics.clickhouse.enabled", havingValue = "true")
public class RevenueQueryService {

    private final JdbcTemplate clickHouseJdbcTemplate;

    public RevenueQueryService(JdbcTemplate clickHouseJdbcTemplate) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    // =============================================================
    // Core Dashboard Metrics
    // =============================================================

    public RevenueMetrics getRevenueMetrics() {
        try {
            var now = LocalDate.now();
            var monthStart = now.withDayOfMonth(1);
            var lastMonthStart = monthStart.minusMonths(1);
            var thirtyDaysAgo = now.minusDays(30);

            // MRR: sum of active subscription amounts normalized to monthly
            BigDecimal mrr = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COALESCE(SUM(
                                CASE
                                    WHEN billing_cycle = 'ANNUAL' THEN amount / 12
                                    ELSE amount
                                END
                            ), 0)
                            FROM revenue_events
                            WHERE event_type IN ('subscription.created', 'subscription.renewed')
                              AND event_timestamp >= toDateTime64(?)
                            """, BigDecimal.class,
                    monthStart.atStartOfDay().toString());

            // ARR
            BigDecimal arr = mrr != null ? mrr.multiply(BigDecimal.valueOf(12)) : BigDecimal.ZERO;

            // Revenue today
            BigDecimal todayRevenue = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COALESCE(SUM(amount), 0)
                            FROM revenue_events
                            WHERE event_type IN ('subscription.created', 'subscription.renewed')
                              AND toDate(event_timestamp) = today()
                            """, BigDecimal.class);

            // Revenue this month
            BigDecimal monthRevenue = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COALESCE(SUM(amount), 0)
                            FROM revenue_events
                            WHERE event_type IN ('subscription.created', 'subscription.renewed')
                              AND event_timestamp >= toDateTime64(?)
                            """, BigDecimal.class,
                    monthStart.atStartOfDay().toString());

            // Active subscriptions (distinct users with recent create/renewal)
            Long activeSubs = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COUNT(DISTINCT user_id)
                            FROM revenue_events
                            WHERE event_type IN ('subscription.created', 'subscription.renewed')
                              AND event_timestamp >= toDateTime64(?)
                            """, Long.class,
                    thirtyDaysAgo.atStartOfDay().toString());

            // New subscriptions today
            Long newToday = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM revenue_events
                            WHERE event_type = 'subscription.created'
                              AND toDate(event_timestamp) = today()
                            """, Long.class);

            // Cancelled today
            Long cancelledToday = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM revenue_events
                            WHERE event_type = 'subscription.cancelled'
                              AND toDate(event_timestamp) = today()
                            """, Long.class);

            // Renewed today
            Long renewedToday = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM revenue_events
                            WHERE event_type = 'subscription.renewed'
                              AND toDate(event_timestamp) = today()
                            """, Long.class);

            // Churn rate (last 30 days): cancellations / total active
            Long cancellations30d = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COUNT(*)
                            FROM revenue_events
                            WHERE event_type = 'subscription.cancelled'
                              AND event_timestamp >= toDateTime64(?)
                            """, Long.class,
                    thirtyDaysAgo.atStartOfDay().toString());

            double churnRate = (activeSubs != null && activeSubs > 0 && cancellations30d != null)
                    ? (cancellations30d.doubleValue() / activeSubs.doubleValue()) * 100
                    : 0.0;

            // ARPU
            BigDecimal arpu = (activeSubs != null && activeSubs > 0 && mrr != null)
                    ? mrr.divide(BigDecimal.valueOf(activeSubs), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // MRR last month (for growth calculation)
            BigDecimal lastMonthMrr = clickHouseJdbcTemplate.queryForObject("""
                            SELECT COALESCE(SUM(
                                CASE
                                    WHEN billing_cycle = 'ANNUAL' THEN amount / 12
                                    ELSE amount
                                END
                            ), 0)
                            FROM revenue_events
                            WHERE event_type IN ('subscription.created', 'subscription.renewed')
                              AND event_timestamp >= toDateTime64(?)
                              AND event_timestamp < toDateTime64(?)
                            """, BigDecimal.class,
                    lastMonthStart.atStartOfDay().toString(),
                    monthStart.atStartOfDay().toString());

            double mrrGrowth = (lastMonthMrr != null && lastMonthMrr.compareTo(BigDecimal.ZERO) > 0 && mrr != null)
                    ? mrr.subtract(lastMonthMrr).divide(lastMonthMrr, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            return new RevenueMetrics(
                    mrr != null ? mrr : BigDecimal.ZERO,
                    arr,
                    todayRevenue != null ? todayRevenue : BigDecimal.ZERO,
                    monthRevenue != null ? monthRevenue : BigDecimal.ZERO,
                    activeSubs != null ? activeSubs : 0,
                    newToday != null ? newToday : 0,
                    cancelledToday != null ? cancelledToday : 0,
                    renewedToday != null ? renewedToday : 0,
                    churnRate,
                    arpu,
                    mrrGrowth
            );

        } catch (Exception e) {
            log.error("Error fetching revenue metrics from ClickHouse: {}", e.getMessage());
            return RevenueMetrics.empty();
        }
    }

    // =============================================================
    // Revenue breakdown by plan
    // =============================================================

    public List<RevenueByPlan> getRevenueByPlan(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    plan_type,
                    COUNT(DISTINCT user_id) as sub_count,
                    COALESCE(SUM(
                        CASE
                            WHEN billing_cycle = 'ANNUAL' THEN amount / 12
                            ELSE amount
                        END
                    ), 0) as mrr,
                    COALESCE(SUM(amount), 0) as total_revenue
                FROM revenue_events
                WHERE event_type IN ('subscription.created', 'subscription.renewed')
                  AND toDate(event_timestamp) >= ?
                  AND toDate(event_timestamp) <= ?
                GROUP BY plan_type
                ORDER BY total_revenue DESC
                """;

        var rows = clickHouseJdbcTemplate.queryForList(sql, from.toString(), to.toString());
        BigDecimal grandTotal = BigDecimal.ZERO;

        // First pass: calculate grand total
        for (var row : rows) {
            BigDecimal total = (BigDecimal) row.get("total_revenue");
            if (total != null) grandTotal = grandTotal.add(total);
        }

        List<RevenueByPlan> result = new ArrayList<>();
        for (var row : rows) {
            String planType = (String) row.get("plan_type");
            Long subCount = ((Number) row.get("sub_count")).longValue();
            BigDecimal mrr = (BigDecimal) row.get("mrr");
            BigDecimal totalRevenue = (BigDecimal) row.get("total_revenue");
            double percentOfTotal = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? totalRevenue.divide(grandTotal, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            result.add(new RevenueByPlan(planType, subCount, mrr, totalRevenue, percentOfTotal));
        }

        return result;
    }

    // =============================================================
    // Revenue breakdown by payment provider
    // =============================================================

    public List<RevenueByProvider> getRevenueByProvider(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    provider,
                    COUNT(*) as tx_count,
                    COALESCE(SUM(amount), 0) as total_amount,
                    COALESCE(SUM(CASE WHEN event_type = 'payment.refunded' THEN amount ELSE 0 END), 0) as refunded
                FROM revenue_events
                WHERE toDate(event_timestamp) >= ?
                  AND toDate(event_timestamp) <= ?
                GROUP BY provider
                ORDER BY total_amount DESC
                """;

        var rows = clickHouseJdbcTemplate.queryForList(sql, from.toString(), to.toString());
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (var row : rows) {
            BigDecimal total = (BigDecimal) row.get("total_amount");
            if (total != null) grandTotal = grandTotal.add(total);
        }

        List<RevenueByProvider> result = new ArrayList<>();
        for (var row : rows) {
            String provider = (String) row.get("provider");
            long txCount = ((Number) row.get("tx_count")).longValue();
            BigDecimal totalAmount = (BigDecimal) row.get("total_amount");
            BigDecimal refunded = (BigDecimal) row.get("refunded");
            double percentOfTotal = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? totalAmount.divide(grandTotal, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            result.add(new RevenueByProvider(provider, txCount, totalAmount, refunded, percentOfTotal));
        }

        return result;
    }

    // =============================================================
    // Daily revenue trend (for charts)
    // =============================================================

    public List<DailyRevenueTrend> getDailyRevenueTrend(int days) {
        String sql = """
                SELECT
                    toDate(event_timestamp) as date,
                    COALESCE(SUM(CASE
                        WHEN event_type IN ('subscription.created', 'subscription.renewed') THEN amount
                        ELSE 0
                    END), 0) as revenue,
                    COUNT(CASE WHEN event_type = 'subscription.created' THEN 1 END) as new_subs,
                    COUNT(CASE WHEN event_type = 'subscription.cancelled' THEN 1 END) as cancellations
                FROM revenue_events
                WHERE event_timestamp >= toDateTime64(?)
                GROUP BY date
                ORDER BY date ASC
                """;

        var startDate = LocalDate.now().minusDays(days).atStartOfDay().toString();

        var rows = clickHouseJdbcTemplate.queryForList(sql, startDate);
        List<DailyRevenueTrend> result = new ArrayList<>();

        for (var row : rows) {
            Object dateObj = row.get("date");
            LocalDate date = (dateObj instanceof LocalDate ld) ? ld : LocalDate.parse(dateObj.toString());
            BigDecimal revenue = (BigDecimal) row.get("revenue");
            long newSubs = ((Number) row.get("new_subs")).longValue();
            long cancellations = ((Number) row.get("cancellations")).longValue();

            result.add(new DailyRevenueTrend(date, revenue, newSubs, cancellations));
        }

        return result;
    }

    // =============================================================
    // Top plans by revenue
    // =============================================================

    public List<RevenueByPlan> getTopPlans(int limit) {
        String sql = """
                SELECT
                    plan_type,
                    COUNT(DISTINCT user_id) as sub_count,
                    COALESCE(SUM(
                        CASE
                            WHEN billing_cycle = 'ANNUAL' THEN amount / 12
                            ELSE amount
                        END
                    ), 0) as mrr,
                    COALESCE(SUM(amount), 0) as total_revenue
                FROM revenue_events
                WHERE event_type IN ('subscription.created', 'subscription.renewed')
                  AND event_timestamp >= toDateTime64(?)
                GROUP BY plan_type
                ORDER BY total_revenue DESC
                LIMIT ?
                """;

        var monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay().toString();
        var rows = clickHouseJdbcTemplate.queryForList(sql, monthStart, limit);

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (var row : rows) {
            BigDecimal total = (BigDecimal) row.get("total_revenue");
            if (total != null) grandTotal = grandTotal.add(total);
        }

        List<RevenueByPlan> result = new ArrayList<>();
        for (var row : rows) {
            String planType = (String) row.get("plan_type");
            Long subCount = ((Number) row.get("sub_count")).longValue();
            BigDecimal mrr = (BigDecimal) row.get("mrr");
            BigDecimal totalRevenue = (BigDecimal) row.get("total_revenue");
            double percentOfTotal = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? totalRevenue.divide(grandTotal, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            result.add(new RevenueByPlan(planType, subCount, mrr, totalRevenue, percentOfTotal));
        }

        return result;
    }
}
