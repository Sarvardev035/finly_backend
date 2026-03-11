package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.AccountType;
import com.finly.backend.domain.model.CardType;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateAccountRequest;
import com.finly.backend.dto.request.UpdateAccountRequest;
import com.finly.backend.dto.response.AccountResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.util.List;

@Route(value = "admin/accounts", layout = AdminMainLayout.class)
@PageTitle("Accounts | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminAccountsView extends AdminBaseListView<AccountResponse> {

    private final AdminPanelService adminPanelService;
    private final VaadinSecurityContext securityContext;
    private ComboBox<User> userSelector;
    private User selectedUser;

    private TextField nameField;
    private ComboBox<AccountType> typeField;
    private ComboBox<Currency> currencyField;
    private NumberField initialBalanceField;
    private TextField cardNumberField;
    private ComboBox<CardType> cardTypeField;
    private TextField expiryDateField;
    private AccountResponse currentAccount;

    public AdminAccountsView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        super(apiClient, AccountResponse.class, "/api/accounts", "Account Management");
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;
        try {
            addComponentAtIndex(1, buildUserSelectorBar());
            preloadUsers();
        } catch (Exception ex) {
            Notification.show("Accounts page initialization failed: " + ex.getMessage(),
                    4000, Notification.Position.TOP_END);
            securityContext.setActAsUserId(null);
            grid.setItems(List.of());
        }
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        grid.removeAllColumns();
        grid.addColumn(a -> a.getId() == null ? "-" : a.getId().toString()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(AccountResponse::getName).setHeader("Name").setSortable(true);
        grid.addColumn(a -> a.getType() == null ? "-" : a.getType().name()).setHeader("Type");
        grid.addColumn(a -> a.getCurrency() == null ? "-" : a.getCurrency().name()).setHeader("Currency");
        grid.addColumn(a -> a.getBalance() == null ? "0" : a.getBalance().toPlainString()).setHeader("Balance")
                .setSortable(true);
        grid.addColumn(a -> a.getCardNumber() == null ? "-" : a.getCardNumber()).setHeader("Card Number");
        grid.addColumn(a -> a.getCardType() == null ? "-" : a.getCardType().name()).setHeader("Card Type");
        grid.addColumn(a -> a.getExpiryDate() == null ? "-" : a.getExpiryDate()).setHeader("Expiry");
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
        Button add = new Button("Add Account");
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
    protected void buildForm(AccountResponse account) {
        this.currentAccount = cloneAccount(account);

        nameField = new TextField("Account Name");
        nameField.setValue(account.getName() != null ? account.getName() : "");

        typeField = new ComboBox<>("Account Type");
        typeField.setItems(AccountType.values());
        typeField.setValue(account.getType());

        currencyField = new ComboBox<>("Currency");
        currencyField.setItems(Currency.values());
        currencyField.setValue(account.getCurrency() != null ? account.getCurrency() : Currency.USD);

        initialBalanceField = new NumberField("Initial Balance");
        initialBalanceField.setValue(account.getBalance() == null ? 0.0 : account.getBalance().doubleValue());
        initialBalanceField.setStep(0.01);

        cardNumberField = new TextField("Card Number (Optional)");
        cardNumberField.setClearButtonVisible(true);
        cardNumberField.setValue(account.getCardNumber() != null ? account.getCardNumber() : "");

        cardTypeField = new ComboBox<>("Card Type (Optional)");
        cardTypeField.setItems(CardType.values());
        cardTypeField.setClearButtonVisible(true);
        cardTypeField.setValue(account.getCardType());

        expiryDateField = new TextField("Expiry Date (Optional, MM/YY)");
        expiryDateField.setClearButtonVisible(true);
        expiryDateField.setValue(account.getExpiryDate() != null ? account.getExpiryDate() : "");

        boolean isEdit = account.getId() != null;
        typeField.setReadOnly(isEdit);
        initialBalanceField.setReadOnly(isEdit);
        cardNumberField.setReadOnly(isEdit);
        cardTypeField.setReadOnly(isEdit);
        expiryDateField.setReadOnly(isEdit);

        form.add(nameField, typeField, currencyField, initialBalanceField, cardNumberField, cardTypeField,
                expiryDateField);
    }

    @Override
    protected AccountResponse getFormEntity() {
        currentAccount.setName(nameField.getValue());
        currentAccount.setType(typeField.getValue());
        currentAccount.setCurrency(currencyField.getValue());
        Double val = initialBalanceField.getValue();
        currentAccount.setBalance(val == null ? BigDecimal.ZERO : BigDecimal.valueOf(val));

        String cNum = cardNumberField.getValue();
        currentAccount.setCardNumber(cNum == null || cNum.isBlank() ? null : cNum.trim());

        currentAccount.setCardType(cardTypeField.getValue());

        String exp = expiryDateField.getValue();
        currentAccount.setExpiryDate(exp == null || exp.isBlank() ? null : exp.trim());
        return currentAccount;
    }

    @Override
    protected String getEntityId(AccountResponse entity) {
        return entity == null || entity.getId() == null ? null : entity.getId().toString();
    }

    @Override
    protected void handleSave(AccountResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }

        String cardNumber = normalize(entity.getCardNumber());
        String expiryDate = normalize(entity.getExpiryDate());
        CardType cardType = entity.getCardType();
        UI ui = UI.getCurrent();

        try {
            if (entity.getId() == null) {
                CreateAccountRequest req = CreateAccountRequest.builder()
                        .name(entity.getName())
                        .type(entity.getType())
                        .currency(entity.getCurrency())
                        .initialBalance(entity.getBalance() == null ? BigDecimal.ZERO : entity.getBalance())
                        .cardNumber(cardNumber)
                        .cardType(cardType)
                        .expiryDate(expiryDate)
                        .build();
                apiClient.post("/api/accounts", req, AccountResponse.class)
                        .subscribe(saved -> runOnUi(ui, () -> {
                            Notification.show("Account successfully added", 2000, Notification.Position.TOP_END);
                            formDialog.close();
                            updateList();
                        }), err -> runOnUi(ui, () -> showSaveError(entity, err)));
            } else {
                UpdateAccountRequest req = UpdateAccountRequest.builder()
                        .name(entity.getName())
                        .currency(entity.getCurrency())
                        .build();
                apiClient.put("/api/accounts/" + entity.getId(), req, AccountResponse.class)
                        .subscribe(updated -> runOnUi(ui, () -> {
                            Notification.show("Account successfully updated", 2000, Notification.Position.TOP_END);
                            formDialog.close();
                            updateList();
                        }), err -> runOnUi(ui, () -> showSaveError(entity, err)));
            }
        } catch (Exception err) {
            Notification.show((entity.getId() == null ? "Create" : "Update") + " failed: " + err.getMessage(), 3000,
                    Notification.Position.TOP_END);
        }
    }

    @Override
    protected void deleteEntity(AccountResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }
        if (entity == null || entity.getId() == null) {
            Notification.show("Invalid account id", 2000, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete account?");
        confirm.add("Account \"" + entity.getName() + "\" will be permanently deleted.");

        Button cancel = new Button("Cancel", e -> confirm.close());
        Button yesDelete = new Button("Delete", e -> {
            try {
                apiClient.delete("/api/accounts/" + entity.getId())
                        .subscribe(
                                v -> {
                                },
                                err -> runOnUi(ui, () -> Notification.show("Delete failed: " + cleanErrorMessage(err), 4000,
                                        Notification.Position.TOP_END)),
                                () -> runOnUi(ui, () -> {
                            confirm.close();
                            Notification.show("Account successfully deleted", 2000, Notification.Position.TOP_END);
                            updateList();
                        }));
            } catch (Exception err) {
                Notification.show("Delete failed: " + err.getMessage(), 3000, Notification.Position.TOP_END);
            }
        });
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
            String name = user.getFullName() == null ? "No Name" : user.getFullName();
            return name + " (" + user.getEmail() + ")";
        });
        userSelector.setWidth("460px");
        userSelector.setPlaceholder("Choose a user");
        userSelector.addValueChangeListener(event -> {
            try {
                selectedUser = event.getValue();
                if (selectedUser != null) {
                    securityContext.setActAsUserId(selectedUser.getId().toString());
                } else {
                    securityContext.setActAsUserId(null);
                }
                refreshAccountsAfterUserSelect();
            } catch (Exception ex) {
                Notification.show("Failed to switch user: " + ex.getMessage(),
                        3000, Notification.Position.TOP_END);
            }
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
            grid.setItems(java.util.List.of());
            Notification.show("No users found", 2000, Notification.Position.TOP_END);
        }
    }

    private void refreshAccountsAfterUserSelect() {
        grid.setItems(List.of());
        countLabel.setText("Loading...");
        updateList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AccountResponse cloneAccount(AccountResponse source) {
        if (source == null) {
            return new AccountResponse();
        }
        return AccountResponse.builder()
                .id(source.getId())
                .name(source.getName())
                .type(source.getType())
                .currency(source.getCurrency())
                .balance(source.getBalance())
                .cardNumber(source.getCardNumber())
                .cardType(source.getCardType())
                .expiryDate(source.getExpiryDate())
                .createdAt(source.getCreatedAt())
                .userFullName(source.getUserFullName())
                .build();
    }

    private void showSaveError(AccountResponse entity, Throwable err) {
        Notification.show((entity.getId() == null ? "Create" : "Update") + " failed: " + cleanErrorMessage(err), 5000,
                Notification.Position.TOP_END);
    }

    private String cleanErrorMessage(Throwable err) {
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
