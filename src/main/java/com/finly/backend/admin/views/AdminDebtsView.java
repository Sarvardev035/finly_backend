package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.DebtStatus;
import com.finly.backend.domain.model.DebtType;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateDebtRequest;
import com.finly.backend.dto.request.RepaymentRequest;
import com.finly.backend.dto.response.AccountResponse;
import com.finly.backend.dto.response.DebtResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/debts", layout = AdminMainLayout.class)
@PageTitle("Debts | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminDebtsView extends AdminBaseListView<DebtResponse> {

    private final AdminPanelService adminPanelService;
    private final VaadinSecurityContext securityContext;
    private ComboBox<User> userSelector;
    private ComboBox<DebtType> typeFilter;
    private ComboBox<DebtStatus> statusFilter;
    private User selectedUser;

    private TextField personField;
    private ComboBox<DebtType> typeField;
    private ComboBox<Currency> currencyField;
    private ComboBox<AccountResponse> accountField;
    private BigDecimalField amountField;
    private TextField descriptionField;
    private DatePicker dueDateField;
    private DebtResponse currentDebt;

    public AdminDebtsView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        super(apiClient, DebtResponse.class, "/api/debts", "Debt Management");
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;
        addComponentAtIndex(1, buildUserSelectorBar());
        preloadUsers();
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        grid.removeAllColumns();
        grid.addColumn(d -> d.getId() == null ? "-" : d.getId().toString()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(DebtResponse::getPersonName).setHeader("Person").setSortable(true);
        grid.addColumn(d -> d.getType() == null ? "-" : d.getType().name()).setHeader("Type");
        grid.addColumn(DebtResponse::getAmount).setHeader("Amount").setSortable(true);
        grid.addColumn(d -> d.getCurrency() == null ? "-" : d.getCurrency().name()).setHeader("Currency");
        grid.addColumn(d -> d.getAccountName() == null ? "-" : d.getAccountName()).setHeader("Account");
        grid.addColumn(DebtResponse::getRemainingAmount).setHeader("Remaining");
        grid.addColumn(d -> d.getStatus() == null ? "-" : d.getStatus().name()).setHeader("Status");
        grid.addColumn(DebtResponse::getDueDate).setHeader("Due Date").setSortable(true);
        grid.addColumn(d -> d.getDescription() == null ? "-" : d.getDescription()).setHeader("Description");
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> editEntity(item));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            Button repay = new Button("Repay", e -> openRepayDialog(item));
            repay.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
            repay.setEnabled(item.getStatus() == DebtStatus.OPEN);
            Button delete = new Button("Delete", e -> deleteEntity(item));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(edit, repay, delete);
        }).setHeader("Actions");
    }

    @Override
    protected Button createAddButton() {
        Button add = new Button("Add Debt");
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
    protected void buildForm(DebtResponse debt) {
        currentDebt = cloneDebt(debt);

        personField = new TextField("Person Name");
        personField.setValue(currentDebt.getPersonName() == null ? "" : currentDebt.getPersonName());

        typeField = new ComboBox<>("Type");
        typeField.setItems(DebtType.values());
        typeField.setValue(currentDebt.getType() == null ? DebtType.DEBT : currentDebt.getType());

        currencyField = new ComboBox<>("Currency");
        currencyField.setItems(Currency.values());
        currencyField.setValue(currentDebt.getCurrency() == null ? Currency.USD : currentDebt.getCurrency());

        accountField = new ComboBox<>("Account (optional)");
        accountField.setWidthFull();
        accountField.setClearButtonVisible(true);
        accountField.setItemLabelGenerator(a -> a.getName() == null ? "-" : a.getName());
        loadAccountsForCreate(accountField, currentDebt.getAccountId());

        amountField = new BigDecimalField("Amount");
        amountField.setValue(currentDebt.getAmount());

        descriptionField = new TextField("Description");
        descriptionField.setPlaceholder("Optional");
        descriptionField.setValue(currentDebt.getDescription() == null ? "" : currentDebt.getDescription());

        dueDateField = new DatePicker("Due Date");
        dueDateField.setValue(currentDebt.getDueDate() == null ? LocalDate.now().plusDays(7) : currentDebt.getDueDate());

        boolean isEdit = currentDebt.getId() != null;
        personField.setReadOnly(isEdit);
        typeField.setReadOnly(isEdit);
        currencyField.setReadOnly(isEdit);
        accountField.setReadOnly(isEdit);
        amountField.setReadOnly(isEdit);
        descriptionField.setReadOnly(isEdit);
        dueDateField.setReadOnly(isEdit);

        form.add(personField, typeField, currencyField, accountField, amountField, descriptionField, dueDateField);
    }

    @Override
    protected DebtResponse getFormEntity() {
        currentDebt.setPersonName(personField.getValue());
        currentDebt.setType(typeField.getValue());
        currentDebt.setCurrency(currencyField.getValue());
        currentDebt.setAccountId(accountField.getValue() == null ? null : accountField.getValue().getId());
        currentDebt.setAccountName(accountField.getValue() == null ? null : accountField.getValue().getName());
        currentDebt.setAmount(amountField.getValue());
        currentDebt.setDescription(normalizeDescription(descriptionField.getValue()));
        currentDebt.setDueDate(dueDateField.getValue());
        return currentDebt;
    }

    @Override
    protected String getEntityId(DebtResponse entity) {
        return entity == null || entity.getId() == null ? null : entity.getId().toString();
    }

    @Override
    protected void handleSave(DebtResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }

        if (entity.getId() != null) {
            Notification.show("Debt edit is not supported by API. Use Repay or Delete.", 3500,
                    Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        CreateDebtRequest req = CreateDebtRequest.builder()
                .personName(entity.getPersonName())
                .type(entity.getType())
                .currency(entity.getCurrency())
                .accountId(entity.getAccountId())
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .dueDate(entity.getDueDate())
                .build();

        apiClient.post("/api/debts", req, DebtResponse.class)
                .subscribe(saved -> runOnUi(ui, () -> {
                    Notification.show("Debt successfully added", 2000, Notification.Position.TOP_END);
                    formDialog.close();
                    updateList();
                }), err -> runOnUi(ui, () -> Notification.show("Create failed: " + cleanError(err), 4000,
                        Notification.Position.TOP_END)));
    }

    @Override
    protected void deleteEntity(DebtResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }
        if (entity == null || entity.getId() == null) {
            Notification.show("Invalid debt id", 2000, Notification.Position.TOP_END);
            return;
        }
        UI ui = UI.getCurrent();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete debt?");
        confirm.add("Debt will be permanently deleted.");
        Button cancel = new Button("Cancel", e -> confirm.close());
        Button yesDelete = new Button("Delete", e -> apiClient.delete("/api/debts/" + entity.getId())
                .subscribe(v -> {
                }, err -> runOnUi(ui, () -> Notification.show("Delete failed: " + cleanError(err), 4000,
                        Notification.Position.TOP_END)), () -> runOnUi(ui, () -> {
                            confirm.close();
                            Notification.show("Debt deleted", 2000, Notification.Position.TOP_END);
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
        String path = "/api/debts";
        if (typeFilter != null && typeFilter.getValue() != null) {
            path += "?type=" + typeFilter.getValue().name();
        }
        if (statusFilter != null && statusFilter.getValue() != null) {
            path += (path.contains("?") ? "&" : "?") + "status=" + statusFilter.getValue().name();
        }

        UI ui = UI.getCurrent();
        apiClient.getArray(path, DebtResponse[].class)
                .subscribe(items -> runOnUi(ui, () -> {
                    List<DebtResponse> list = items == null ? List.of() : Arrays.asList(items);
                    grid.setItems(list);
                    countLabel.setText("Total items: " + list.size());
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Failed to load debts: " + cleanError(err), 4000, Notification.Position.TOP_END)));
    }

    private HorizontalLayout buildUserSelectorBar() {
        userSelector = new ComboBox<>("Select User");
        userSelector.setItemLabelGenerator(user -> {
            String name = user.getFullName() == null ? "No Name" : user.getFullName();
            return name + " (" + user.getEmail() + ")";
        });
        userSelector.setWidth("460px");
        userSelector.setPlaceholder("Choose a user");
        userSelector.addValueChangeListener(event -> {
            selectedUser = event.getValue();
            securityContext.setActAsUserId(selectedUser == null ? null : selectedUser.getId().toString());
            refreshAfterUserSelect();
        });

        typeFilter = new ComboBox<>("Type");
        typeFilter.setItems(DebtType.values());
        typeFilter.setClearButtonVisible(true);
        typeFilter.addValueChangeListener(event -> refreshAfterUserSelect());

        statusFilter = new ComboBox<>("Status");
        statusFilter.setItems(DebtStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(event -> refreshAfterUserSelect());

        HorizontalLayout bar = new HorizontalLayout(userSelector, typeFilter, statusFilter);
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

    private void openRepayDialog(DebtResponse debt) {
        if (debt == null || debt.getId() == null) {
            Notification.show("Invalid debt", 2000, Notification.Position.TOP_END);
            return;
        }
        if (debt.getStatus() == DebtStatus.CLOSED) {
            Notification.show("Debt already closed", 2000, Notification.Position.TOP_END);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Repay Debt");
        dialog.setWidth("460px");

        BigDecimalField paymentField = new BigDecimalField("Payment Amount");
        paymentField.setValue(debt.getRemainingAmount());

        ComboBox<AccountResponse> accountField = new ComboBox<>("Account (optional)");
        accountField.setWidthFull();
        accountField.setClearButtonVisible(true);
        accountField.setItemLabelGenerator(a -> a.getName() == null ? "-" : a.getName());
        loadAccountsForRepay(accountField);

        VerticalLayout body = new VerticalLayout(paymentField, accountField);
        body.setPadding(false);
        body.setSpacing(true);
        dialog.add(body);

        Button cancel = new Button("Cancel", e -> dialog.close());
        Button repay = new Button("Repay", e -> {
            if (paymentField.getValue() == null || paymentField.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                Notification.show("Valid payment amount required", 2500, Notification.Position.TOP_END);
                return;
            }
            repayDebt(dialog, debt.getId(), paymentField.getValue(),
                    accountField.getValue() == null ? null : accountField.getValue().getId());
        });
        repay.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancel, repay);
        dialog.open();
    }

    private void loadAccountsForRepay(ComboBox<AccountResponse> accountField) {
        UI ui = UI.getCurrent();
        apiClient.getArray("/api/accounts", AccountResponse[].class)
                .subscribe(accounts -> runOnUi(ui, () -> {
                    List<AccountResponse> list = accounts == null ? List.of() : Arrays.asList(accounts);
                    accountField.setItems(list);
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Failed to load accounts: " + cleanError(err), 4000, Notification.Position.TOP_END)));
    }

    private void loadAccountsForCreate(ComboBox<AccountResponse> accountField, UUID selectedAccountId) {
        UI ui = UI.getCurrent();
        apiClient.getArray("/api/accounts", AccountResponse[].class)
                .subscribe(accounts -> runOnUi(ui, () -> {
                    List<AccountResponse> list = accounts == null ? List.of() : Arrays.asList(accounts);
                    accountField.setItems(list);
                    if (selectedAccountId != null) {
                        list.stream()
                                .filter(a -> selectedAccountId.equals(a.getId()))
                                .findFirst()
                                .ifPresent(accountField::setValue);
                    }
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Failed to load accounts: " + cleanError(err), 4000, Notification.Position.TOP_END)));
    }

    private void repayDebt(Dialog dialog, UUID debtId, BigDecimal paymentAmount, UUID accountId) {
        UI ui = UI.getCurrent();
        RepaymentRequest req = RepaymentRequest.builder()
                .paymentAmount(paymentAmount)
                .accountId(accountId)
                .build();
        apiClient.post("/api/debts/" + debtId + "/repay", req, DebtResponse.class)
                .subscribe(resp -> runOnUi(ui, () -> {
                    dialog.close();
                    Notification.show("Debt repaid successfully", 2000, Notification.Position.TOP_END);
                    updateList();
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Repay failed: " + cleanError(err), 4000, Notification.Position.TOP_END)));
    }

    private DebtResponse cloneDebt(DebtResponse source) {
        if (source == null) {
            return new DebtResponse();
        }
        return DebtResponse.builder()
                .id(source.getId())
                .personName(source.getPersonName())
                .type(source.getType())
                .amount(source.getAmount())
                .remainingAmount(source.getRemainingAmount())
                .currency(source.getCurrency())
                .accountId(source.getAccountId())
                .accountName(source.getAccountName())
                .description(source.getDescription())
                .status(source.getStatus())
                .dueDate(source.getDueDate())
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
