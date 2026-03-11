package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateExpenseRequest;
import com.finly.backend.dto.request.UpdateExpenseRequest;
import com.finly.backend.dto.response.AccountResponse;
import com.finly.backend.dto.response.CategoryResponse;
import com.finly.backend.dto.response.ExpenseResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/expenses", layout = AdminMainLayout.class)
@PageTitle("Expenses | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminExpensesView extends AdminBaseListView<ExpenseResponse> {

    private final AdminPanelService adminPanelService;
    private final VaadinSecurityContext securityContext;
    private ComboBox<User> userSelector;
    private User selectedUser;

    private BigDecimalField amountField;
    private ComboBox<Currency> currencyField;
    private DatePicker dateField;
    private ComboBox<AccountResponse> accountField;
    private ComboBox<CategoryResponse> categoryField;
    private TextField descriptionField;
    private ExpenseResponse currentExpense;

    public AdminExpensesView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        super(apiClient, ExpenseResponse.class, "/api/expenses", "Expense Management");
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;
        addComponentAtIndex(1, buildUserSelectorBar());
        preloadUsers();
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        grid.removeAllColumns();
        grid.addColumn(e -> e.getId() == null ? "-" : e.getId().toString()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(ExpenseResponse::getAmount).setHeader("Amount").setSortable(true);
        grid.addColumn(e -> e.getCurrency() == null ? "-" : e.getCurrency().name()).setHeader("Currency");
        grid.addColumn(ExpenseResponse::getExpenseDate).setHeader("Date").setSortable(true);
        grid.addColumn(ExpenseResponse::getDescription).setHeader("Description").setSortable(true);
        grid.addColumn(e -> e.getAccountName() == null ? "-" : e.getAccountName()).setHeader("Account");
        grid.addColumn(e -> e.getCategoryName() == null ? "-" : e.getCategoryName()).setHeader("Category");
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> editEntity(item));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            Button delete = new Button("Delete", e -> deleteEntity(item));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions");
    }

    @Override
    protected Button createAddButton() {
        Button add = new Button("Add Expense");
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.addClickListener(e -> {
            if (selectedUser == null) {
                Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
                return;
            }
            addEntity();
        });
        return add;
    }

    @Override
    protected void buildForm(ExpenseResponse expense) {
        currentExpense = cloneExpense(expense);

        amountField = new BigDecimalField("Amount");
        amountField.setValue(currentExpense.getAmount());

        currencyField = new ComboBox<>("Currency");
        currencyField.setItems(Currency.values());
        currencyField.setValue(currentExpense.getCurrency() == null ? Currency.USD : currentExpense.getCurrency());

        dateField = new DatePicker("Date");
        dateField.setValue(currentExpense.getExpenseDate() == null ? LocalDate.now() : currentExpense.getExpenseDate());

        descriptionField = new TextField("Description");
        descriptionField.setPlaceholder("Optional");
        descriptionField.setValue(currentExpense.getDescription() == null ? "" : currentExpense.getDescription());

        accountField = new ComboBox<>("Account");
        accountField.setWidthFull();
        accountField.setItemLabelGenerator(a -> a.getName() == null ? "-" : a.getName());

        categoryField = new ComboBox<>("Category");
        categoryField.setWidthFull();
        categoryField.setItemLabelGenerator(c -> c.getName() == null ? "-" : c.getName());

        loadAccountsForForm(currentExpense.getAccountId());
        loadCategoriesForForm(currentExpense.getCategoryId());

        form.add(amountField, currencyField, dateField, accountField, categoryField, descriptionField);
    }

    @Override
    protected ExpenseResponse getFormEntity() {
        currentExpense.setAmount(amountField.getValue());
        currentExpense.setCurrency(currencyField.getValue());
        currentExpense.setExpenseDate(dateField.getValue());
        currentExpense.setDescription(normalizeDescription(descriptionField.getValue()));
        currentExpense.setAccountId(accountField.getValue() == null ? null : accountField.getValue().getId());
        currentExpense.setAccountName(accountField.getValue() == null ? null : accountField.getValue().getName());
        currentExpense.setCategoryId(categoryField.getValue() == null ? null : categoryField.getValue().getId());
        currentExpense.setCategoryName(categoryField.getValue() == null ? null : categoryField.getValue().getName());
        return currentExpense;
    }

    @Override
    protected String getEntityId(ExpenseResponse entity) {
        return entity == null || entity.getId() == null ? null : entity.getId().toString();
    }

    @Override
    protected void handleSave(ExpenseResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        if (entity.getId() == null) {
            CreateExpenseRequest req = CreateExpenseRequest.builder()
                    .amount(entity.getAmount())
                    .currency(entity.getCurrency())
                    .description(entity.getDescription())
                    .expenseDate(entity.getExpenseDate())
                    .categoryId(entity.getCategoryId())
                    .accountId(entity.getAccountId())
                    .build();
            apiClient.post("/api/expenses", req, ExpenseResponse.class)
                    .subscribe(saved -> runOnUi(ui, () -> {
                        Notification.show("Expense successfully added", 2000, Notification.Position.TOP_END);
                        formDialog.close();
                        updateList();
                    }), err -> runOnUi(ui, () -> Notification.show("Create failed: " + cleanErrorMessage(err), 4000,
                            Notification.Position.TOP_END)));
            return;
        }

        UpdateExpenseRequest req = UpdateExpenseRequest.builder()
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .expenseDate(entity.getExpenseDate())
                .categoryId(entity.getCategoryId())
                .accountId(entity.getAccountId())
                .build();
        apiClient.put("/api/expenses/" + entity.getId(), req, ExpenseResponse.class)
                .subscribe(updated -> runOnUi(ui, () -> {
                    Notification.show("Expense successfully updated", 2000, Notification.Position.TOP_END);
                    formDialog.close();
                    updateList();
                }), err -> runOnUi(ui, () -> Notification.show("Update failed: " + cleanErrorMessage(err), 4000,
                        Notification.Position.TOP_END)));
    }

    @Override
    protected void deleteEntity(ExpenseResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }
        if (entity == null || entity.getId() == null) {
            Notification.show("Invalid expense id", 2000, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete expense?");
        confirm.add("Expense \"" + entity.getDescription() + "\" will be permanently deleted.");

        Button cancel = new Button("Cancel", e -> confirm.close());
        Button yesDelete = new Button("Delete", e -> apiClient.delete("/api/expenses/" + entity.getId())
                .subscribe(v -> {
                }, err -> runOnUi(ui, () -> Notification.show("Delete failed: " + cleanErrorMessage(err), 4000,
                        Notification.Position.TOP_END)), () -> runOnUi(ui, () -> {
                            confirm.close();
                            Notification.show("Expense successfully deleted", 2000, Notification.Position.TOP_END);
                            updateList();
                        })));
        yesDelete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        confirm.getFooter().add(cancel, yesDelete);
        confirm.open();
    }

    @Override
    protected void updateList() {
        if (selectedUser == null) {
            grid.setItems(List.of());
            countLabel.setText("Total items: 0");
            return;
        }
        super.updateList();
    }

    private HorizontalLayout buildUserSelectorBar() {
        userSelector = new ComboBox<>("Select User");
        userSelector.setItemLabelGenerator(user -> {
            String fullName = user.getFullName() == null ? "No Name" : user.getFullName();
            return fullName + " (" + user.getEmail() + ")";
        });
        userSelector.setWidth("460px");
        userSelector.setPlaceholder("Choose a user");
        userSelector.addValueChangeListener(event -> {
            selectedUser = event.getValue();
            if (selectedUser != null) {
                securityContext.setActAsUserId(selectedUser.getId().toString());
            } else {
                securityContext.setActAsUserId(null);
            }
            refreshAfterUserSelect();
        });

        HorizontalLayout bar = new HorizontalLayout(userSelector);
        bar.setWidthFull();
        return bar;
    }

    private void preloadUsers() {
        var users = adminPanelService.findUsers("", PageRequest.of(0, 200)).getContent();
        userSelector.setItems(users);
        if (users.isEmpty()) {
            securityContext.setActAsUserId(null);
            grid.setItems(List.of());
            Notification.show("No users found", 2000, Notification.Position.TOP_END);
        }
    }

    private void refreshAfterUserSelect() {
        grid.setItems(List.of());
        countLabel.setText("Loading...");
        updateList();
    }

    private void loadAccountsForForm(UUID selectedAccountId) {
        UI ui = UI.getCurrent();
        apiClient.getArray("/api/accounts", AccountResponse[].class)
                .subscribe(accounts -> runOnUi(ui, () -> {
                    List<AccountResponse> accountList = accounts == null ? List.of() : List.of(accounts);
                    accountField.setItems(accountList);
                    if (selectedAccountId != null) {
                        accountList.stream()
                                .filter(acc -> selectedAccountId.equals(acc.getId()))
                                .findFirst()
                                .ifPresent(accountField::setValue);
                    }
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Failed to load accounts: " + cleanErrorMessage(err), 4000, Notification.Position.TOP_END)));
    }

    private void loadCategoriesForForm(UUID selectedCategoryId) {
        UI ui = UI.getCurrent();
        apiClient.getArray("/api/categories", CategoryResponse[].class)
                .subscribe(categories -> runOnUi(ui, () -> {
                    List<CategoryResponse> categoryList = categories == null ? List.of() : List.of(categories);
                    categoryField.setItems(categoryList);
                    if (selectedCategoryId != null) {
                        categoryList.stream()
                                .filter(cat -> selectedCategoryId.equals(cat.getId()))
                                .findFirst()
                                .ifPresent(categoryField::setValue);
                    }
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Failed to load categories: " + cleanErrorMessage(err), 4000, Notification.Position.TOP_END)));
    }

    private ExpenseResponse cloneExpense(ExpenseResponse source) {
        if (source == null) {
            return new ExpenseResponse();
        }
        return ExpenseResponse.builder()
                .id(source.getId())
                .amount(source.getAmount())
                .currency(source.getCurrency())
                .description(source.getDescription())
                .expenseDate(source.getExpenseDate())
                .categoryId(source.getCategoryId())
                .categoryName(source.getCategoryName())
                .accountId(source.getAccountId())
                .accountName(source.getAccountName())
                .userName(source.getUserName())
                .createdAt(source.getCreatedAt())
                .build();
    }

    private String cleanErrorMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return "Unknown error";
        }
        return error.getMessage();
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return "No description";
        }
        return value.trim();
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
