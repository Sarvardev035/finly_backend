package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.response.AnalyticsSummaryResponse;
import com.finly.backend.dto.response.ExpenseCategoryAmountResponse;
import com.finly.backend.dto.response.IncomeVsExpenseResponse;
import com.finly.backend.dto.response.MonthlyAmountResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

@Route(value = "admin/analytics", layout = AdminMainLayout.class)
@PageTitle("Analytics | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminAnalyticsView extends VerticalLayout {

    private final AdminApiClient apiClient;
    private final AdminPanelService adminPanelService;
    private final VaadinSecurityContext securityContext;

    private ComboBox<User> userSelector;
    private DatePicker startDate;
    private DatePicker endDate;

    private Span incomeValue;
    private Span expenseValue;
    private Span balanceValue;
    private Span savingsRateValue;
    private Div categoriesChart;
    private Div timeseriesChart;
    private Div monthlyChart;

    public AdminAnalyticsView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        this.apiClient = apiClient;
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;

        addClassName("admin-page");
        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(new H2("Analytics"));
        add(buildFilters());
        add(buildSummaryCards());
        add(buildCharts());

        preloadUsers();
    }

    private HorizontalLayout buildFilters() {
        userSelector = new ComboBox<>("Select User");
        userSelector.setWidth("420px");
        userSelector.setItemLabelGenerator(user -> {
            String name = user.getFullName() == null ? "No Name" : user.getFullName();
            return name + " (" + user.getEmail() + ")";
        });
        userSelector.addValueChangeListener(event -> {
            User selected = event.getValue();
            securityContext.setActAsUserId(selected == null ? null : selected.getId().toString());
            fetchAnalytics();
        });

        LocalDate now = LocalDate.now();
        startDate = new DatePicker("Start Date");
        startDate.setValue(now.minusMonths(1));
        endDate = new DatePicker("End Date");
        endDate.setValue(now);

        Button apply = new Button("Apply", e -> fetchAnalytics());
        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout filters = new HorizontalLayout(userSelector, startDate, endDate, apply);
        filters.setWidthFull();
        filters.setAlignItems(Alignment.END);
        return filters;
    }

    private HorizontalLayout buildSummaryCards() {
        incomeValue = new Span("0");
        expenseValue = new Span("0");
        balanceValue = new Span("0");
        savingsRateValue = new Span("0%");

        HorizontalLayout cards = new HorizontalLayout(
                statCard("Total Income", incomeValue, "income"),
                statCard("Total Expense", expenseValue, "expense"),
                statCard("Balance", balanceValue, "balance"),
                statCard("Savings Rate", savingsRateValue, "savings"));
        cards.setWidthFull();
        cards.addClassName("analytics-summary-row");
        return cards;
    }

    private Div statCard(String title, Span value, String variant) {
        Div card = new Div();
        card.addClassName("admin-card");
        card.addClassName("analytics-stat-card");
        card.addClassName("analytics-stat-" + variant);
        card.getStyle().set("flex", "1");
        Span titleSpan = new Span(title);
        titleSpan.addClassName("analytics-stat-title");
        value.addClassName("analytics-stat-value");
        card.add(titleSpan, value);
        return card;
    }

    private HorizontalLayout buildCharts() {
        categoriesChart = createChartContainer("Category Breakdown");
        timeseriesChart = createChartContainer("Income vs Expense");
        monthlyChart = createChartContainer("Monthly Expenses");
        HorizontalLayout row = new HorizontalLayout(categoriesChart, timeseriesChart, monthlyChart);
        row.setWidthFull();
        row.setHeight("520px");
        row.setFlexGrow(1, categoriesChart, timeseriesChart, monthlyChart);
        return row;
    }

    private Div createChartContainer(String title) {
        Div container = new Div(new H3(title));
        container.addClassName("admin-card");
        container.addClassName("analytics-chart-card");
        container.setSizeFull();
        container.getStyle().set("padding", "1rem");
        return container;
    }

    private void preloadUsers() {
        var users = adminPanelService.findUsers("", PageRequest.of(0, 200)).getContent();
        userSelector.setItems(users);
        if (!users.isEmpty()) {
            userSelector.setValue(users.get(0));
        } else {
            Notification.show("No users found", 2000, Notification.Position.TOP_END);
        }
    }

    private void fetchAnalytics() {
        if (userSelector.getValue() == null) {
            return;
        }
        if (startDate.getValue() == null || endDate.getValue() == null) {
            Notification.show("Start and end dates are required", 2500, Notification.Position.TOP_END);
            return;
        }

        String start = startDate.getValue().toString();
        String end = endDate.getValue().toString();
        UI ui = UI.getCurrent();
        categoriesChart.removeAll();
        categoriesChart.add(new H3("Category Breakdown"), new Span("Loading..."));
        timeseriesChart.removeAll();
        timeseriesChart.add(new H3("Income vs Expense"), new Span("Loading..."));
        monthlyChart.removeAll();
        monthlyChart.add(new H3("Monthly Expenses"), new Span("Loading..."));

        apiClient.get("/api/analytics/summary", AnalyticsSummaryResponse.class)
                .subscribe(data -> runOnUi(ui, () -> {
                    incomeValue.setText(data.getTotalIncome() == null ? "0" : data.getTotalIncome().toPlainString());
                    expenseValue.setText(data.getTotalExpense() == null ? "0" : data.getTotalExpense().toPlainString());
                    balanceValue.setText(data.getTotalBalance() == null ? "0" : data.getTotalBalance().toPlainString());
                    savingsRateValue.setText(data.getSavings() == null ? "0" : data.getSavings().toPlainString());
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Dashboard analytics failed: " + cleanError(err), 4000, Notification.Position.TOP_END)));

        apiClient.getList("/api/analytics/expenses-by-category?from=" + start + "&to=" + end,
                new ParameterizedTypeReference<List<ExpenseCategoryAmountResponse>>() {
                }).subscribe(data -> runOnUi(ui, () -> renderCategories(data)),
                        err -> runOnUi(ui, () -> Notification.show(
                                "Category analytics failed: " + cleanError(err), 4000, Notification.Position.TOP_END)));

        apiClient.getList("/api/analytics/income-vs-expense?from=" + start + "&to=" + end,
                new ParameterizedTypeReference<List<IncomeVsExpenseResponse>>() {
                }).subscribe(data -> runOnUi(ui, () -> renderTimeSeries(data)),
                        err -> runOnUi(ui, () -> Notification.show(
                                "Timeseries analytics failed: " + cleanError(err), 4000, Notification.Position.TOP_END)));

        apiClient.getList("/api/analytics/monthly-expenses?from=" + start + "&to=" + end,
                new ParameterizedTypeReference<List<MonthlyAmountResponse>>() {
                }).subscribe(data -> runOnUi(ui, () -> renderMonthlyExpenses(data)),
                        err -> runOnUi(ui, () -> Notification.show(
                                "Monthly analytics failed: " + cleanError(err), 4000, Notification.Position.TOP_END)));
    }

    private void renderCategories(List<ExpenseCategoryAmountResponse> data) {
        categoriesChart.removeAll();
        categoriesChart.add(new H3("Category Breakdown"));
        if (data == null || data.isEmpty()) {
            categoriesChart.add(new Span("No data"));
            return;
        }

        java.math.BigDecimal total = data.stream()
                .map(d -> d.getAmount() == null ? java.math.BigDecimal.ZERO : d.getAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        for (ExpenseCategoryAmountResponse stat : data) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setJustifyContentMode(JustifyContentMode.BETWEEN);
            java.math.BigDecimal amount = stat.getAmount() == null ? java.math.BigDecimal.ZERO : stat.getAmount();
            java.math.BigDecimal percent = total.compareTo(java.math.BigDecimal.ZERO) == 0
                    ? java.math.BigDecimal.ZERO
                    : amount.multiply(new java.math.BigDecimal("100"))
                            .divide(total, 2, java.math.RoundingMode.HALF_UP);
            row.add(new Span(stat.getCategory()),
                    new Span(percent.toPlainString() + "%"));

            Div bg = new Div();
            bg.addClassName("analytics-category-bar-bg");
            bg.setWidthFull();
            bg.setHeight("10px");

            Div fill = new Div();
            fill.addClassName("analytics-category-bar-fill");
            fill.setWidth(percent.toPlainString() + "%");
            bg.add(fill);

            categoriesChart.add(row, bg);
        }
    }

    private void renderTimeSeries(List<IncomeVsExpenseResponse> data) {
        timeseriesChart.removeAll();
        timeseriesChart.add(new H3("Income vs Expense"));
        if (data == null || data.isEmpty()) {
            timeseriesChart.add(new Span("No data"));
            return;
        }

        HorizontalLayout bars = new HorizontalLayout();
        bars.setWidthFull();
        bars.setHeight("420px");
        bars.setAlignItems(Alignment.END);
        bars.getStyle().set("overflow-x", "auto");

        for (IncomeVsExpenseResponse point : data) {
            VerticalLayout stack = new VerticalLayout();
            stack.setPadding(false);
            stack.setSpacing(false);
            stack.setAlignItems(Alignment.CENTER);
            stack.setWidth("64px");

            int inH = Math.max(2, point.getIncome() == null ? 2 : point.getIncome().intValue() / 5);
            int exH = Math.max(2, point.getExpense() == null ? 2 : point.getExpense().intValue() / 5);

            Div inBar = new Div();
            inBar.addClassName("analytics-income-bar");
            inBar.setWidth("14px");
            inBar.setHeight(Math.min(280, inH) + "px");

            Div exBar = new Div();
            exBar.addClassName("analytics-expense-bar");
            exBar.setWidth("14px");
            exBar.setHeight(Math.min(280, exH) + "px");

            HorizontalLayout pair = new HorizontalLayout(inBar, exBar);
            pair.setSpacing(true);
            pair.setAlignItems(Alignment.END);

            String label = point.getMonth() == null ? "-" : point.getMonth();
            Span date = new Span(label.length() > 10 ? label.substring(label.length() - 10) : label);
            date.getStyle().set("font-size", "0.7rem");

            stack.add(pair, date);
            bars.add(stack);
        }
        timeseriesChart.add(bars);
    }

    private void renderMonthlyExpenses(List<MonthlyAmountResponse> data) {
        monthlyChart.removeAll();
        monthlyChart.add(new H3("Monthly Expenses"));
        if (data == null || data.isEmpty()) {
            monthlyChart.add(new Span("No data"));
            return;
        }
        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        for (MonthlyAmountResponse row : data) {
            HorizontalLayout line = new HorizontalLayout();
            line.setWidthFull();
            line.setJustifyContentMode(JustifyContentMode.BETWEEN);
            line.add(new Span(row.getMonth()), new Span(row.getAmount() == null ? "0" : row.getAmount().toPlainString()));
            list.add(line);
        }
        monthlyChart.add(list);
    }

    private String cleanError(Throwable err) {
        if (err == null || err.getMessage() == null || err.getMessage().isBlank()) {
            return "Unknown error";
        }
        return err.getMessage();
    }

    private void runOnUi(UI ui, Command action) {
        if (ui == null || action == null) {
            return;
        }
        if (ui.getSession() == null) {
            return;
        }
        ui.access(action);
    }
}
