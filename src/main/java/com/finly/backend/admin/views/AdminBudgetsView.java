package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.BudgetType;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateBudgetRequest;
import com.finly.backend.dto.response.BudgetResponse;
import com.finly.backend.dto.response.CategoryResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Route(value = "admin/budgets", layout = AdminMainLayout.class)
@PageTitle("Budgets | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminBudgetsView extends VerticalLayout {

    private final AdminApiClient apiClient;
    private final AdminPanelService adminPanelService;
    private final VaadinSecurityContext securityContext;

    private ComboBox<User> userSelector;
    private IntegerField yearField;
    private IntegerField monthField;
    private final Grid<BudgetResponse> grid = new Grid<>(BudgetResponse.class, false);

    public AdminBudgetsView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        this.apiClient = apiClient;
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;

        addClassName("admin-page");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Budget Management"), buildTopBar(), buildGrid());
        preloadUsers();
    }

    private HorizontalLayout buildTopBar() {
        userSelector = new ComboBox<>("Select User");
        userSelector.setWidth("420px");
        userSelector.setItemLabelGenerator(user -> {
            String name = user.getFullName() == null ? "No Name" : user.getFullName();
            return name + " (" + user.getEmail() + ")";
        });

        LocalDate now = LocalDate.now();
        yearField = new IntegerField("Year");
        yearField.setWidth("120px");
        yearField.setValue(now.getYear());

        monthField = new IntegerField("Month");
        monthField.setWidth("100px");
        monthField.setMin(1);
        monthField.setMax(12);
        monthField.setValue(now.getMonthValue());

        Button load = new Button("Load");
        load.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        load.addClickListener(e -> loadBudgets());

        Button add = new Button("Add Budget");
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.addClickListener(e -> openBudgetForm(null));

        userSelector.addValueChangeListener(event -> {
            User selected = event.getValue();
            securityContext.setActAsUserId(selected == null ? null : selected.getId().toString());
            loadBudgets();
        });

        HorizontalLayout bar = new HorizontalLayout(userSelector, yearField, monthField, load, add);
        bar.setWidthFull();
        bar.setAlignItems(Alignment.END);
        return bar;
    }

    private Grid<BudgetResponse> buildGrid() {
        grid.setSizeFull();
        grid.addColumn(b -> b.getId() == null ? "-" : b.getId().toString()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(b -> b.getType() == null ? "-" : b.getType().name()).setHeader("Type");
        grid.addColumn(b -> b.getCategoryName() == null ? "-" : b.getCategoryName()).setHeader("Category");
        grid.addColumn(BudgetResponse::getMonthlyLimit).setHeader("Limit");
        grid.addColumn(BudgetResponse::getSpentAmount).setHeader("Spent");
        grid.addColumn(BudgetResponse::getRemainingAmount).setHeader("Remaining");
        grid.addColumn(BudgetResponse::getPercentageUsed).setHeader("% Used");
        grid.addColumn(BudgetResponse::getYear).setHeader("Year");
        grid.addColumn(BudgetResponse::getMonth).setHeader("Month");
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openBudgetForm(item));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button("Delete", e -> deleteBudget(item));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions");
        return grid;
    }

    private void preloadUsers() {
        var users = adminPanelService.findUsers("", PageRequest.of(0, 200)).getContent();
        userSelector.setItems(users);
        if (users.isEmpty()) {
            Notification.show("No users found", 2000, Notification.Position.TOP_END);
        }
    }

    private void loadBudgets() {
        User selected = userSelector.getValue();
        Integer year = yearField.getValue();
        Integer month = monthField.getValue();
        if (selected == null) {
            grid.setItems(List.of());
            return;
        }
        if (year == null || month == null || month < 1 || month > 12) {
            Notification.show("Valid year/month required", 2500, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        apiClient.getArray("/api/budgets?year=" + year + "&month=" + month, BudgetResponse[].class)
                .subscribe(items -> runOnUi(ui, () -> {
                    List<BudgetResponse> list = items == null ? List.of() : Arrays.asList(items);
                    grid.setItems(list);
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Failed to load budgets: " + cleanError(err), 4000, Notification.Position.TOP_END)));
    }

    private void openBudgetForm(BudgetResponse editing) {
        if (userSelector.getValue() == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(editing == null ? "Add Budget" : "Edit Budget");
        dialog.setWidth("480px");

        ComboBox<BudgetType> typeField = new ComboBox<>("Type");
        typeField.setItems(BudgetType.values());
        typeField.setValue(editing == null || editing.getType() == null ? BudgetType.EXPENSE : editing.getType());

        ComboBox<CategoryResponse> categoryField = new ComboBox<>("Category (optional)");
        categoryField.setItemLabelGenerator(c -> c.getName() == null ? "-" : c.getName());
        categoryField.setClearButtonVisible(true);
        categoryField.setWidthFull();
        loadCategoriesForDialog(categoryField, editing == null ? null : editing.getCategoryId());

        BigDecimalField limitField = new BigDecimalField("Monthly Limit");
        limitField.setValue(editing == null ? null : editing.getMonthlyLimit());

        IntegerField yearInput = new IntegerField("Year");
        yearInput.setValue(editing == null ? yearField.getValue() : editing.getYear());

        IntegerField monthInput = new IntegerField("Month");
        monthInput.setMin(1);
        monthInput.setMax(12);
        monthInput.setValue(editing == null ? monthField.getValue() : editing.getMonth());

        VerticalLayout body = new VerticalLayout(typeField, categoryField, limitField, yearInput, monthInput);
        body.setPadding(false);
        body.setSpacing(true);
        dialog.add(body);

        Button cancel = new Button("Cancel", e -> dialog.close());
        Button save = new Button("Save", e -> {
            if (typeField.getValue() == null || limitField.getValue() == null
                    || yearInput.getValue() == null || monthInput.getValue() == null) {
                Notification.show("Type, limit, year, month are required", 2500, Notification.Position.TOP_END);
                return;
            }
            CreateBudgetRequest req = CreateBudgetRequest.builder()
                    .type(typeField.getValue())
                    .categoryId(categoryField.getValue() == null ? null : categoryField.getValue().getId())
                    .monthlyLimit(limitField.getValue())
                    .year(yearInput.getValue())
                    .month(monthInput.getValue())
                    .build();
            saveBudget(dialog, req);
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void saveBudget(Dialog dialog, CreateBudgetRequest req) {
        UI ui = UI.getCurrent();
        apiClient.post("/api/budgets", req, BudgetResponse.class)
                .subscribe(saved -> runOnUi(ui, () -> {
                    dialog.close();
                    Notification.show("Budget saved", 2000, Notification.Position.TOP_END);
                    loadBudgets();
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Save failed: " + cleanError(err), 4000, Notification.Position.TOP_END)));
    }

    private void deleteBudget(BudgetResponse budget) {
        if (budget == null || budget.getId() == null) {
            Notification.show("Invalid budget", 2000, Notification.Position.TOP_END);
            return;
        }
        UI ui = UI.getCurrent();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete budget?");
        confirm.add("Budget will be permanently deleted.");

        Button cancel = new Button("Cancel", e -> confirm.close());
        Button yes = new Button("Delete", e -> apiClient.delete("/api/budgets/" + budget.getId())
                .subscribe(v -> {
                }, err -> runOnUi(ui, () -> Notification.show(
                        "Delete failed: " + cleanError(err), 4000, Notification.Position.TOP_END)),
                        () -> runOnUi(ui, () -> {
                            confirm.close();
                            Notification.show("Budget deleted", 2000, Notification.Position.TOP_END);
                            loadBudgets();
                        })));
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirm.getFooter().add(cancel, yes);
        confirm.open();
    }

    private void loadCategoriesForDialog(ComboBox<CategoryResponse> categoryField, java.util.UUID selectedCategoryId) {
        UI ui = UI.getCurrent();
        apiClient.getArray("/api/categories", CategoryResponse[].class)
                .subscribe(categories -> runOnUi(ui, () -> {
                    List<CategoryResponse> list = categories == null ? List.of() : Arrays.asList(categories);
                    categoryField.setItems(list);
                    if (selectedCategoryId != null) {
                        list.stream()
                                .filter(c -> selectedCategoryId.equals(c.getId()))
                                .findFirst()
                                .ifPresent(categoryField::setValue);
                    }
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Failed to load categories: " + cleanError(err), 4000, Notification.Position.TOP_END)));
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
