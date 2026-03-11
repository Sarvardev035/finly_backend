package com.finly.backend.admin.views;

import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.response.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "admin", layout = AdminMainLayout.class)
@RouteAlias(value = "admin/dashboard", layout = AdminMainLayout.class)
@PageTitle("Dashboard | Finly Admin")
@RolesAllowed("ADMIN")
@Slf4j
public class AdminDashboardView extends VerticalLayout {

    private final AdminApiClient apiClient;
    private final AdminPanelService adminPanelService;
    private Span incomeSpan;
    private Span expenseSpan;
    private Span balanceSpan;
    private Span accountsSpan;
    private Div trendsChart;
    private Div categoriesChart;
    private Grid<User> usersGrid;

    public AdminDashboardView(AdminApiClient apiClient, AdminPanelService adminPanelService) {
        this.apiClient = apiClient;
        this.adminPanelService = adminPanelService;
        addClassName("admin-page");
        setSpacing(true);
        setPadding(true);

        add(new H2("Dashboard Overview"));

        HorizontalLayout statsRow = new HorizontalLayout();
        statsRow.setWidthFull();

        incomeSpan = new Span("$0.00");
        expenseSpan = new Span("$0.00");
        balanceSpan = new Span("$0.00");
        accountsSpan = new Span("0");

        statsRow.add(createStatCard("Total Income", incomeSpan, "primary"));
        statsRow.add(createStatCard("Total Expenses", expenseSpan, "error"));
        statsRow.add(createStatCard("Current Balance", balanceSpan, "success"));
        statsRow.add(createStatCard("Total Accounts", accountsSpan, "contrast"));
        add(statsRow);

        HorizontalLayout chartsRow = new HorizontalLayout();
        chartsRow.setWidthFull();
        chartsRow.setMinHeight("400px");

        trendsChart = createPlaceholderChart("Loading trends...");
        Div trendsContainer = new Div(new H3("Financial Trends"), trendsChart);
        trendsContainer.addClassName("admin-card");
        trendsContainer.getStyle().set("flex", "2");

        categoriesChart = createPlaceholderChart("Loading categories...");
        Div categoriesContainer = new Div(new H3("Expense Categories"), categoriesChart);
        categoriesContainer.addClassName("admin-card");
        categoriesContainer.getStyle().set("flex", "1");

        chartsRow.add(trendsContainer, categoriesContainer);
        add(chartsRow);

        add(buildUsersSection());

        fetchData();
        fetchUsers();
    }

    private void fetchData() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(1);

        // Fetch Summary
        apiClient.get("/api/analytics/dashboard?startDate=" + start + "&endDate=" + end, DashboardSummaryResponse.class)
                .subscribe(response -> getUI().ifPresent(ui -> ui.access(() -> {
                    incomeSpan.setText("$" + response.getTotalIncome());
                    expenseSpan.setText("$" + response.getTotalExpense());
                    balanceSpan.setText("$" + response.getBalance());
                })));

        // Fetch Accounts Count
        apiClient.getList("/api/accounts", new ParameterizedTypeReference<List<AccountResponse>>() {
        })
                .subscribe(response -> getUI().ifPresent(ui -> ui.access(() -> {
                    accountsSpan.setText(String.valueOf(response.size()));
                })));

        // Fetch Category Stats
        apiClient.getList("/api/analytics/categories?type=EXPENSE&startDate=" + start + "&endDate=" + end,
                new ParameterizedTypeReference<List<CategoryStatsResponse>>() {
                })
                .subscribe(response -> getUI().ifPresent(ui -> ui.access(() -> {
                    renderCategoryChart(response);
                })));

        // Fetch Time Series
        apiClient.getList("/api/analytics/timeseries?period=DAY&startDate=" + start + "&endDate=" + end,
                new ParameterizedTypeReference<List<TimeSeriesResponse>>() {
                })
                .subscribe(response -> getUI().ifPresent(ui -> ui.access(() -> {
                    renderTrendsChart(response);
                })));
    }

    private Div buildUsersSection() {
        Div container = new Div();
        container.addClassName("admin-card");
        container.getStyle().set("padding", "1rem");
        container.getStyle().set("margin-top", "1rem");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        H3 title = new H3("All Users");
        title.getStyle().set("margin", "0");
        Button openUsers = new Button("Open User Management",
                e -> getUI().ifPresent(ui -> ui.navigate("admin/users")));
        header.add(title, openUsers);
        header.expand(title);

        usersGrid = new Grid<>(User.class, false);
        usersGrid.setWidthFull();
        usersGrid.setHeight("280px");
        usersGrid.addColumn(User::getFullName).setHeader("Full Name").setAutoWidth(true);
        usersGrid.addColumn(User::getEmail).setHeader("Email").setAutoWidth(true);
        usersGrid.addColumn(u -> u.getRole().name()).setHeader("Role").setAutoWidth(true);
        usersGrid.addColumn(
                u -> u.getCreatedAt() == null ? "-" : u.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setHeader("Created")
                .setAutoWidth(true);
        usersGrid.addItemDoubleClickListener(
                e -> getUI().ifPresent(ui -> ui.navigate("admin/users/" + e.getItem().getId())));

        container.add(header, usersGrid);
        return container;
    }

    private void fetchUsers() {
        var page = adminPanelService.findUsers("", PageRequest.of(0, 20));
        usersGrid.setItems(page.getContent());
    }

    private void renderCategoryChart(List<CategoryStatsResponse> data) {
        categoriesChart.removeAll();
        if (data == null || data.isEmpty()) {
            categoriesChart.add(new Span("No data available"));
            return;
        }

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);
        for (CategoryStatsResponse stat : data) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setJustifyContentMode(JustifyContentMode.BETWEEN);
            row.add(new Span(stat.getCategoryName()), new Span(stat.getPercentage() + "%"));

            Div barBg = new Div();
            barBg.setWidthFull();
            barBg.setHeight("8px");
            barBg.getStyle().set("background", "#eee");
            barBg.getStyle().set("border-radius", "4px");
            barBg.getStyle().set("margin-bottom", "1rem");

            Div barFill = new Div();
            barFill.setWidth(stat.getPercentage() + "%");
            barFill.setHeight("8px");
            barFill.getStyle().set("background", "#1a237e");
            barFill.getStyle().set("border-radius", "4px");
            barBg.add(barFill);

            list.add(row, barBg);
        }
        categoriesChart.add(list);
    }

    private void renderTrendsChart(List<TimeSeriesResponse> data) {
        trendsChart.removeAll();
        if (data == null || data.isEmpty()) {
            trendsChart.add(new Span("No data available"));
            return;
        }

        HorizontalLayout barContainer = new HorizontalLayout();
        barContainer.setWidthFull();
        barContainer.setHeight("300px");
        barContainer.setAlignItems(Alignment.BASELINE);
        barContainer.setSpacing(true);
        barContainer.setPadding(true);
        barContainer.getStyle().set("overflow-x", "auto");

        for (TimeSeriesResponse point : data) {
            VerticalLayout barStack = new VerticalLayout();
            barStack.setPadding(false);
            barStack.setSpacing(false);
            barStack.setAlignItems(Alignment.CENTER);
            barStack.setWidth("40px");

            int incomeHeight = point.getIncomeAmount().intValue() / 10; // Simple scaling
            int expenseHeight = point.getExpenseAmount().intValue() / 10;

            Div incomeBar = new Div();
            incomeBar.setWidth("12px");
            incomeBar.setHeight(Math.min(250, incomeHeight) + "px");
            incomeBar.getStyle().set("background", "#2e7d32");
            incomeBar.getStyle().set("border-radius", "2px 2px 0 0");
            incomeBar.getElement().setAttribute("title", "Income: " + point.getIncomeAmount());

            Div expenseBar = new Div();
            expenseBar.setWidth("12px");
            expenseBar.setHeight(Math.min(250, expenseHeight) + "px");
            expenseBar.getStyle().set("background", "#c62828");
            expenseBar.getStyle().set("border-radius", "2px 2px 0 0");
            expenseBar.getElement().setAttribute("title", "Expense: " + point.getExpenseAmount());

            HorizontalLayout bars = new HorizontalLayout(incomeBar, expenseBar);
            bars.setSpacing(false);
            bars.setAlignItems(Alignment.BASELINE);

            String labelText = point.getPeriod();
            if (labelText.length() > 5)
                labelText = labelText.substring(labelText.length() - 5);
            Span label = new Span(labelText);
            label.getStyle().set("font-size", "0.6rem");
            label.getStyle().set("color", "#666");

            barStack.add(bars, label);
            barContainer.add(barStack);
        }
        trendsChart.add(barContainer);
    }

    private Div createStatCard(String title, Span valueSpan, String theme) {
        Div card = new Div();
        card.addClassName("admin-card");
        card.getStyle().set("flex", "1");

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("color", "#666");
        titleSpan.getStyle().set("font-size", "0.875rem");

        H2 valueH2 = new H2(valueSpan);
        valueH2.getStyle().set("margin", "0.5rem 0");
        valueH2.getStyle().set("color", getThemeColor(theme));

        card.add(titleSpan, valueH2);
        return card;
    }

    private String getThemeColor(String theme) {
        switch (theme) {
            case "primary":
                return "#1a237e";
            case "error":
                return "#c62828";
            case "success":
                return "#2e7d32";
            case "contrast":
                return "#333";
            default:
                return "#333";
        }
    }

    private Div createPlaceholderChart(String text) {
        Div placeholder = new Div();
        placeholder.setText(text);
        placeholder.getStyle().set("height", "300px");
        placeholder.getStyle().set("background", "#fff");
        placeholder.getStyle().set("display", "flex");
        placeholder.getStyle().set("align-items", "center");
        placeholder.getStyle().set("justify-content", "center");
        placeholder.getStyle().set("border-radius", "8px");
        return placeholder;
    }
}
