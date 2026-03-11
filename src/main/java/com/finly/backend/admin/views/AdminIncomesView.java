package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateIncomeRequest;
import com.finly.backend.dto.request.UpdateIncomeRequest;
import com.finly.backend.dto.response.AccountResponse;
import com.finly.backend.dto.response.CategoryResponse;
import com.finly.backend.dto.response.IncomeResponse;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/incomes", layout = AdminMainLayout.class)
@PageTitle("Incomes | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminIncomesView extends AdminBaseListView<IncomeResponse> {

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
    private IncomeResponse currentIncome;

    public AdminIncomesView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        super(apiClient, IncomeResponse.class, "/api/incomes", "Income Management");
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;
        addComponentAtIndex(1, buildUserSelectorBar());
        preloadUsers();
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        grid.removeAllColumns();
        grid.addColumn(i -> i.getId() == null ? "-" : i.getId().toString()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(IncomeResponse::getAmount).setHeader("Amount").setSortable(true);
        grid.addColumn(i -> i.getCurrency() == null ? "-" : i.getCurrency().name()).setHeader("Currency");
        grid.addColumn(IncomeResponse::getIncomeDate).setHeader("Date").setSortable(true);
        grid.addColumn(IncomeResponse::getDescription).setHeader("Description").setSortable(true);
        grid.addColumn(i -> i.getAccountName() == null ? "-" : i.getAccountName()).setHeader("Account");
        grid.addColumn(i -> i.getCategoryName() == null ? "-" : i.getCategoryName()).setHeader("Category");
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
        Button add = new Button("Add Income");
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
    protected void buildForm(IncomeResponse income) {
        currentIncome = cloneIncome(income);

        amountField = new BigDecimalField("Amount");
        amountField.setValue(currentIncome.getAmount());

        currencyField = new ComboBox<>("Currency");
        currencyField.setItems(Currency.values());
        currencyField.setValue(currentIncome.getCurrency() == null ? Currency.USD : currentIncome.getCurrency());

        dateField = new DatePicker("Date");
        dateField.setValue(currentIncome.getIncomeDate() == null ? LocalDate.now() : currentIncome.getIncomeDate());

        descriptionField = new TextField("Description");
        descriptionField.setPlaceholder("Optional");
        descriptionField.setValue(currentIncome.getDescription() == null ? "" : currentIncome.getDescription());

        accountField = new ComboBox<>("Account");
        accountField.setWidthFull();
        accountField.setItemLabelGenerator(a -> a.getName() == null ? "-" : a.getName());

        categoryField = new ComboBox<>("Category");
        categoryField.setWidthFull();
        categoryField.setItemLabelGenerator(c -> c.getName() == null ? "-" : c.getName());

        loadAccountsForForm(currentIncome.getAccountId());
        loadCategoriesForForm(currentIncome.getCategoryId());

        form.add(amountField, currencyField, dateField, accountField, categoryField, descriptionField);
    }

    @Override
    protected IncomeResponse getFormEntity() {
        currentIncome.setAmount(amountField.getValue());
        currentIncome.setCurrency(currencyField.getValue());
        currentIncome.setIncomeDate(dateField.getValue());
        currentIncome.setDescription(normalizeDescription(descriptionField.getValue()));
        currentIncome.setAccountId(accountField.getValue() == null ? null : accountField.getValue().getId());
        currentIncome.setAccountName(accountField.getValue() == null ? null : accountField.getValue().getName());
        currentIncome.setCategoryId(categoryField.getValue() == null ? null : categoryField.getValue().getId());
        currentIncome.setCategoryName(categoryField.getValue() == null ? null : categoryField.getValue().getName());
        return currentIncome;
    }

    @Override
    protected String getEntityId(IncomeResponse entity) {
        return entity == null || entity.getId() == null ? null : entity.getId().toString();
    }

    @Override
    protected void handleSave(IncomeResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        if (entity.getId() == null) {
            CreateIncomeRequest req = CreateIncomeRequest.builder()
                    .amount(entity.getAmount())
                    .currency(entity.getCurrency())
                    .description(entity.getDescription())
                    .incomeDate(entity.getIncomeDate())
                    .categoryId(entity.getCategoryId())
                    .accountId(entity.getAccountId())
                    .build();
            apiClient.post("/api/incomes", req, IncomeResponse.class)
                    .subscribe(saved -> runOnUi(ui, () -> {
                        Notification.show("Income successfully added", 2000, Notification.Position.TOP_END);
                        formDialog.close();
                        updateList();
                    }), err -> runOnUi(ui, () -> Notification.show("Create failed: " + cleanErrorMessage(err), 4000,
                            Notification.Position.TOP_END)));
            return;
        }

        UpdateIncomeRequest req = UpdateIncomeRequest.builder()
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .incomeDate(entity.getIncomeDate())
                .categoryId(entity.getCategoryId())
                .accountId(entity.getAccountId())
                .build();
        apiClient.put("/api/incomes/" + entity.getId(), req, IncomeResponse.class)
                .subscribe(updated -> runOnUi(ui, () -> {
                    Notification.show("Income successfully updated", 2000, Notification.Position.TOP_END);
                    formDialog.close();
                    updateList();
                }), err -> runOnUi(ui, () -> Notification.show("Update failed: " + cleanErrorMessage(err), 4000,
                        Notification.Position.TOP_END)));
    }

    @Override
    protected void deleteEntity(IncomeResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }
        if (entity == null || entity.getId() == null) {
            Notification.show("Invalid income id", 2000, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete income?");
        confirm.add("Income \"" + entity.getDescription() + "\" will be permanently deleted.");

        Button cancel = new Button("Cancel", e -> confirm.close());
        Button yesDelete = new Button("Delete", e -> apiClient.delete("/api/incomes/" + entity.getId())
                .subscribe(v -> {
                }, err -> runOnUi(ui, () -> Notification.show("Delete failed: " + cleanErrorMessage(err), 4000,
                        Notification.Position.TOP_END)), () -> runOnUi(ui, () -> {
                            confirm.close();
                            Notification.show("Income successfully deleted", 2000, Notification.Position.TOP_END);
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
                    List<AccountResponse> accountList = accounts == null ? List.of() : Arrays.asList(accounts);
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
                    List<CategoryResponse> categoryList = categories == null ? List.of() : Arrays.asList(categories);
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

    private IncomeResponse cloneIncome(IncomeResponse source) {
        if (source == null) {
            return new IncomeResponse();
        }
        return IncomeResponse.builder()
                .id(source.getId())
                .amount(source.getAmount())
                .currency(source.getCurrency())
                .description(source.getDescription())
                .incomeDate(source.getIncomeDate())
                .categoryId(source.getCategoryId())
                .categoryName(source.getCategoryName())
                .accountId(source.getAccountId())
                .accountName(source.getAccountName())
                .userName(source.getUserName())
                .createdAt(source.getCreatedAt())
                .build();
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) {
            return "No description";
        }
        return value.trim();
    }

    private String cleanErrorMessage(Throwable error) {
        if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
            return "Unknown error";
        }
        return error.getMessage();
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
