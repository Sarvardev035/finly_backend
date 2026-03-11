package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateTransferRequest;
import com.finly.backend.dto.response.AccountResponse;
import com.finly.backend.dto.response.TransferResponse;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Route(value = "admin/transfers", layout = AdminMainLayout.class)
@PageTitle("Transfers | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminTransfersView extends AdminBaseListView<TransferResponse> {

    private final AdminPanelService adminPanelService;
    private final VaadinSecurityContext securityContext;
    private ComboBox<User> userSelector;
    private User selectedUser;

    private ComboBox<AccountResponse> fromAccountField;
    private ComboBox<AccountResponse> toAccountField;
    private BigDecimalField amountField;
    private BigDecimalField exchangeRateField;
    private DatePicker dateField;
    private TextField descriptionField;
    private TransferResponse currentTransfer;

    public AdminTransfersView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        super(apiClient, TransferResponse.class, "/api/transfers", "Transfer Management");
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;
        addComponentAtIndex(1, buildUserSelectorBar());
        preloadUsers();
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        grid.removeAllColumns();
        grid.addColumn(t -> t.getId() == null ? "-" : t.getId().toString()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(t -> t.getFromAccountName() == null ? "-" : t.getFromAccountName()).setHeader("From Account");
        grid.addColumn(t -> t.getToAccountName() == null ? "-" : t.getToAccountName()).setHeader("To Account");
        grid.addColumn(TransferResponse::getAmount).setHeader("Amount").setSortable(true);
        grid.addColumn(TransferResponse::getExchangeRate).setHeader("Rate");
        grid.addColumn(TransferResponse::getConvertedAmount).setHeader("Converted Amount");
        grid.addColumn(TransferResponse::getTransferDate).setHeader("Date").setSortable(true);
        grid.addColumn(t -> t.getDescription() == null ? "-" : t.getDescription()).setHeader("Description");
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
        Button add = new Button("Add Transfer");
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
    protected void buildForm(TransferResponse transfer) {
        currentTransfer = cloneTransfer(transfer);

        fromAccountField = new ComboBox<>("From Account");
        fromAccountField.setWidthFull();
        fromAccountField.setItemLabelGenerator(a -> a.getName() == null ? "-" : a.getName());

        toAccountField = new ComboBox<>("To Account");
        toAccountField.setWidthFull();
        toAccountField.setItemLabelGenerator(a -> a.getName() == null ? "-" : a.getName());

        amountField = new BigDecimalField("Amount");
        amountField.setValue(currentTransfer.getAmount());

        exchangeRateField = new BigDecimalField("Exchange Rate (optional)");
        exchangeRateField.setValue(currentTransfer.getExchangeRate());

        dateField = new DatePicker("Date");
        dateField.setValue(currentTransfer.getTransferDate() == null ? LocalDate.now() : currentTransfer.getTransferDate());

        descriptionField = new TextField("Description");
        descriptionField.setPlaceholder("Optional");
        descriptionField.setValue(currentTransfer.getDescription() == null ? "" : currentTransfer.getDescription());

        loadAccountsForForm(currentTransfer.getFromAccountId(), currentTransfer.getToAccountId());

        boolean isEdit = currentTransfer.getId() != null;
        fromAccountField.setReadOnly(isEdit);
        toAccountField.setReadOnly(isEdit);
        amountField.setReadOnly(isEdit);
        exchangeRateField.setReadOnly(isEdit);
        dateField.setReadOnly(isEdit);
        descriptionField.setReadOnly(isEdit);

        form.add(fromAccountField, toAccountField, amountField, exchangeRateField, dateField, descriptionField);
    }

    @Override
    protected TransferResponse getFormEntity() {
        currentTransfer.setFromAccountId(fromAccountField.getValue() == null ? null : fromAccountField.getValue().getId());
        currentTransfer.setFromAccountName(fromAccountField.getValue() == null ? null : fromAccountField.getValue().getName());
        currentTransfer.setToAccountId(toAccountField.getValue() == null ? null : toAccountField.getValue().getId());
        currentTransfer.setToAccountName(toAccountField.getValue() == null ? null : toAccountField.getValue().getName());
        currentTransfer.setAmount(amountField.getValue());
        currentTransfer.setExchangeRate(exchangeRateField.getValue());
        currentTransfer.setTransferDate(dateField.getValue());
        currentTransfer.setDescription(normalizeDescription(descriptionField.getValue()));
        return currentTransfer;
    }

    @Override
    protected String getEntityId(TransferResponse entity) {
        return entity == null || entity.getId() == null ? null : entity.getId().toString();
    }

    @Override
    protected void handleSave(TransferResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }

        if (entity.getId() != null) {
            Notification.show("Transfer edit is not supported by API. Delete and create a new transfer.", 3500,
                    Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        CreateTransferRequest req = CreateTransferRequest.builder()
                .fromAccountId(entity.getFromAccountId())
                .toAccountId(entity.getToAccountId())
                .amount(entity.getAmount())
                .exchangeRate(entity.getExchangeRate())
                .description(entity.getDescription())
                .transferDate(entity.getTransferDate())
                .build();

        apiClient.post("/api/transfers", req, TransferResponse.class)
                .subscribe(saved -> runOnUi(ui, () -> {
                    Notification.show("Transfer successfully added", 2000, Notification.Position.TOP_END);
                    formDialog.close();
                    updateList();
                }), err -> runOnUi(ui, () -> Notification.show("Create failed: " + cleanErrorMessage(err), 4000,
                        Notification.Position.TOP_END)));
    }

    @Override
    protected void deleteEntity(TransferResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }
        if (entity == null || entity.getId() == null) {
            Notification.show("Invalid transfer id", 2000, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete transfer?");
        confirm.add("Transfer will be permanently deleted.");

        Button cancel = new Button("Cancel", e -> confirm.close());
        Button yesDelete = new Button("Delete", e -> apiClient.delete("/api/transfers/" + entity.getId())
                .subscribe(v -> {
                }, err -> runOnUi(ui, () -> Notification.show("Delete failed: " + cleanErrorMessage(err), 4000,
                        Notification.Position.TOP_END)), () -> runOnUi(ui, () -> {
                            confirm.close();
                            Notification.show("Transfer successfully deleted", 2000, Notification.Position.TOP_END);
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

    private void loadAccountsForForm(UUID selectedFrom, UUID selectedTo) {
        UI ui = UI.getCurrent();
        apiClient.getArray("/api/accounts", AccountResponse[].class)
                .subscribe(accounts -> runOnUi(ui, () -> {
                    List<AccountResponse> accountList = accounts == null ? List.of() : Arrays.asList(accounts);
                    fromAccountField.setItems(accountList);
                    toAccountField.setItems(accountList);
                    if (selectedFrom != null) {
                        accountList.stream().filter(a -> selectedFrom.equals(a.getId())).findFirst().ifPresent(fromAccountField::setValue);
                    }
                    if (selectedTo != null) {
                        accountList.stream().filter(a -> selectedTo.equals(a.getId())).findFirst().ifPresent(toAccountField::setValue);
                    }
                }), err -> runOnUi(ui, () -> Notification.show(
                        "Failed to load accounts: " + cleanErrorMessage(err), 4000, Notification.Position.TOP_END)));
    }

    private TransferResponse cloneTransfer(TransferResponse source) {
        if (source == null) {
            return new TransferResponse();
        }
        return TransferResponse.builder()
                .id(source.getId())
                .fromAccountId(source.getFromAccountId())
                .fromAccountName(source.getFromAccountName())
                .toAccountId(source.getToAccountId())
                .toAccountName(source.getToAccountName())
                .amount(source.getAmount())
                .convertedAmount(source.getConvertedAmount())
                .exchangeRate(source.getExchangeRate())
                .description(source.getDescription())
                .transferDate(source.getTransferDate())
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
