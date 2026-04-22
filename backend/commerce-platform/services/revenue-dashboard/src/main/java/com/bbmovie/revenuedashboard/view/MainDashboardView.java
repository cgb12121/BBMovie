package com.bbmovie.revenuedashboard.view;

import com.bbmovie.revenuedashboard.dto.DailyRevenueTrend;
import com.bbmovie.revenuedashboard.dto.RevenueByPlan;
import com.bbmovie.revenuedashboard.dto.RevenueByProvider;
import com.bbmovie.revenuedashboard.dto.RevenueMetrics;
import com.bbmovie.revenuedashboard.service.RevenueQueryService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Route("")
@Secured("ROLE_ADMIN")
public class MainDashboardView extends VerticalLayout {

    private final RevenueQueryService revenueQueryService;

    private Span mrrValue;
    private Span arrValue;
    private Span activeSubsValue;
    private Span churnRateValue;
    private Span todayRevenueValue;
    private Span monthRevenueValue;
    private Span arpuValue;
    private Span mrrGrowthValue;

    private Grid<DailyRevenueTrend> trendGrid;
    private Grid<RevenueByPlan> planGrid;
    private Grid<RevenueByProvider> providerGrid;

    public MainDashboardView(RevenueQueryService revenueQueryService) {
        this.revenueQueryService = revenueQueryService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(buildHeader());
        add(buildKpiCards());
        add(buildTrendTable());
        add(buildBreakdowns());

        refreshData();
    }

    private HorizontalLayout buildHeader() {
        var title = new H2("Revenue Dashboard");
        var lastUpdate = new Span();
        lastUpdate.setId("lastUpdate");
        lastUpdate.getStyle().set("color", "var(--lumo-secondary-text-color)");

        var refreshBtn = new Button("Refresh", e -> refreshData());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var header = new HorizontalLayout(title, lastUpdate, refreshBtn);
        header.setAlignItems(Alignment.CENTER);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setFlexGrow(1, lastUpdate);
        return header;
    }

    private HorizontalLayout buildKpiCards() {
        mrrValue = createSpan("$0");
        arrValue = createSpan("$0");
        activeSubsValue = createSpan("0");
        churnRateValue = createSpan("0%");
        todayRevenueValue = createSpan("$0");
        monthRevenueValue = createSpan("$0");
        arpuValue = createSpan("$0");
        mrrGrowthValue = createSpan("0%");

        var layout = new HorizontalLayout(
                createCard("MRR", mrrValue),
                createCard("ARR", arrValue),
                createCard("Active Subs", activeSubsValue),
                createCard("Churn (30d)", churnRateValue),
                createCard("Revenue Today", todayRevenueValue),
                createCard("Revenue This Month", monthRevenueValue),
                createCard("ARPU", arpuValue),
                createCard("MRR Growth", mrrGrowthValue)
        );
        layout.setWidthFull();
        layout.setFlexGrow(1,
                layout.getComponentAt(0), layout.getComponentAt(1),
                layout.getComponentAt(2), layout.getComponentAt(3),
                layout.getComponentAt(4), layout.getComponentAt(5),
                layout.getComponentAt(6), layout.getComponentAt(7));
        layout.setSpacing(true);
        return layout;
    }

    private Div createCard(String label, Span value) {
        var card = new Div();
        var labelDiv = new Div(label);
        labelDiv.getStyle().set("font-size", "0.875rem")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-xs)");

        value.getStyle().set("font-size", "1.75rem")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)");

        card.add(labelDiv, value);
        card.getStyle().set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("text-align", "center")
                .set("min-width", "120px");
        return card;
    }

    private VerticalLayout buildTrendTable() {
        var section = new VerticalLayout();
        section.setWidthFull();
        section.add(new H3("Daily Revenue Trend (Last 30 Days)"));

        trendGrid = new Grid<>();
        trendGrid.addColumn(DailyRevenueTrend::date).setHeader("Date").setSortable(true);
        trendGrid.addColumn(t -> formatCurrency(t.revenue())).setHeader("Revenue").setSortable(true);
        trendGrid.addColumn(DailyRevenueTrend::newSubscriptions).setHeader("New Subs").setSortable(true);
        trendGrid.addColumn(DailyRevenueTrend::cancellations).setHeader("Cancellations").setSortable(true);
        trendGrid.setAllRowsVisible(true);

        section.add(trendGrid);
        return section;
    }

    private HorizontalLayout buildBreakdowns() {
        var layout = new HorizontalLayout();
        layout.setWidthFull();

        // Revenue by Plan
        var planSection = new VerticalLayout();
        planSection.setWidthFull();
        planSection.add(new H3("Revenue by Plan"));

        planGrid = new Grid<>();
        planGrid.addColumn(RevenueByPlan::planType).setHeader("Plan").setSortable(true);
        planGrid.addColumn(RevenueByPlan::subscriptionCount).setHeader("Subs").setSortable(true);
        planGrid.addColumn(p -> formatCurrency(p.mrr())).setHeader("MRR").setSortable(true);
        planGrid.addColumn(p -> formatCurrency(p.totalRevenue())).setHeader("Total Revenue").setSortable(true);
        planGrid.addColumn(p -> String.format("%.1f%%", p.percentOfTotal())).setHeader("% of Total");
        trendGrid.setAllRowsVisible(true);

        planSection.add(planGrid);

        // Revenue by Provider
        var providerSection = new VerticalLayout();
        providerSection.setWidthFull();
        providerSection.add(new H3("Revenue by Provider"));

        providerGrid = new Grid<>();
        providerGrid.addColumn(RevenueByProvider::provider).setHeader("Provider").setSortable(true);
        providerGrid.addColumn(RevenueByProvider::transactionCount).setHeader("Transactions").setSortable(true);
        providerGrid.addColumn(p -> formatCurrency(p.totalAmount())).setHeader("Total").setSortable(true);
        providerGrid.addColumn(p -> formatCurrency(p.refundedAmount())).setHeader("Refunded");
        providerGrid.addColumn(p -> String.format("%.1f%%", p.percentOfTotal())).setHeader("% of Total");
        trendGrid.setAllRowsVisible(true);

        providerSection.add(providerGrid);

        layout.add(planSection, providerSection);
        layout.setFlexGrow(1, planSection, providerSection);
        layout.setSpacing(true);
        return layout;
    }

    private void refreshData() {
        RevenueMetrics metrics = revenueQueryService.getRevenueMetrics();

        mrrValue.setText(formatCurrency(metrics.mrr()));
        arrValue.setText(formatCurrency(metrics.arr()));
        activeSubsValue.setText(String.valueOf(metrics.activeSubscriptions()));
        churnRateValue.setText(String.format("%.2f%%", metrics.churnRatePercent()));
        todayRevenueValue.setText(formatCurrency(metrics.totalRevenueToday()));
        monthRevenueValue.setText(formatCurrency(metrics.totalRevenueThisMonth()));
        arpuValue.setText(formatCurrency(metrics.arpu()));

        String growthPrefix = metrics.mrrGrowthPercent() >= 0 ? "+" : "";
        mrrGrowthValue.setText(String.format("%s%.2f%%", growthPrefix, metrics.mrrGrowthPercent()));
        mrrGrowthValue.getStyle().set("color",
                metrics.mrrGrowthPercent() >= 0
                        ? "var(--lumo-success-color)"
                        : "var(--lumo-error-color)");

        // Update tables
        List<DailyRevenueTrend> trends = revenueQueryService.getDailyRevenueTrend(30);
        trendGrid.setItems(trends);

        List<RevenueByPlan> plans = revenueQueryService.getTopPlans(10);
        planGrid.setItems(plans);

        List<RevenueByProvider> providers = revenueQueryService.getRevenueByProvider(
                LocalDate.now().minusDays(30), LocalDate.now());
        providerGrid.setItems(providers);

        // Update last update timestamp
        UI.getCurrent().getPage().executeJs(
                "document.getElementById('lastUpdate').textContent = 'Last updated: ' + new Date().toLocaleTimeString()");
    }

    private Span createSpan(String text) {
        return new Span(text);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0";
        return String.format("$%,.2f", amount);
    }
}
