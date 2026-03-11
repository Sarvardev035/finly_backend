package com.finly.backend.admin.views;

import com.finly.backend.admin.dto.AdminAccountForm;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.domain.model.Account;
import com.finly.backend.domain.model.AccountType;
import com.finly.backend.domain.model.CardType;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

@Route(value = "admin/users/:userId", layout = AdminMainLayout.class)
@PageTitle("User Workspace | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminUserWorkspaceView extends VerticalLayout implements BeforeEnterObserver {

    private final AdminPanelService adminPanelService;
    private UUID userId;
    private User user;

    private final Grid<Account> accountGrid = new Grid<>(Account.class, false);
    private final TextField accountSearch = new TextField();

    public AdminUserWorkspaceView(AdminPanelService adminPanelService) {
        this.adminPanelService = adminPanelService;
        addClassName("admin-page");
        setSizeFull();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String rawUserId = event.getRouteParameters().get("userId").orElse(null);
        if (rawUserId == null) {
            event.forwardTo("admin/users");
            return;
        }

        try {
            this.userId = UUID.fromString(rawUserId);
            this.user = adminPanelService.getUser(userId);
        } catch (Exception ex) {
            event.forwardTo("admin/users");
            return;
        }

        removeAll();
        add(buildBreadcrumb(), buildHeader(), buildAccountSection());
        refreshAccounts();
    }

    private Div buildBreadcrumb() {
        Div bread = new Div(new Span("Admin"), new Span(" / "), new Span("Users"), new Span(" / "),
                new Span(user.getEmail()));
        bread.addClassName("admin-breadcrumb");
        return bread;
    }

    private HorizontalLayout buildHeader() {
        H3 title = new H3("User Workspace");
        Span userMeta = new Span(user.getFullName() + " • " + user.getEmail());
        userMeta.addClassName("workspace-meta");

        HorizontalLayout row = new HorizontalLayout(title, userMeta);
        row.setAlignItems(Alignment.BASELINE);
        row.setWidthFull();
        row.expand(title);
        return row;
    }

    private VerticalLayout buildAccountSection() {
        H4 title = new H4("Accounts");

        accountSearch.setPlaceholder("Search account name");
        accountSearch.setClearButtonVisible(true);
        accountSearch.setValueChangeMode(ValueChangeMode.EAGER);
        accountSearch.addValueChangeListener(e -> refreshAccounts());

        Button addAccount = new Button("Add account", e -> openAccountDialog(null));
        addAccount.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout tools = new HorizontalLayout(title, accountSearch, addAccount);
        tools.setAlignItems(Alignment.CENTER);
        tools.setWidthFull();
        tools.expand(title);

        accountGrid.addColumn(Account::getName).setHeader("Name").setSortable(true).setKey("name");
        accountGrid.addColumn(a -> a.getType().name()).setHeader("Type").setAutoWidth(true);
        accountGrid.addColumn(a -> a.getCurrency().name()).setHeader("Currency").setAutoWidth(true);
        accountGrid.addColumn(a -> a.getBalance().toPlainString()).setHeader("Balance").setAutoWidth(true);
        accountGrid.addColumn(new ComponentRenderer<>(this::accountActions)).setHeader("Actions").setAutoWidth(true);
        accountGrid.setSizeFull();

        VerticalLayout section = new VerticalLayout(tools, accountGrid);
        section.setPadding(false);
        section.setSizeFull();
        section.addClassName("admin-section");
        return section;
    }

    private HorizontalLayout accountActions(Account account) {
        Button edit = new Button("Edit", e -> openAccountDialog(account));
        Button delete = new Button("Delete", e -> {
            try {
                adminPanelService.deleteAccountForUser(userId, account.getId());
                Notification.show("Account deleted", 2000, Notification.Position.TOP_END);
                refreshAccounts();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.TOP_END);
            }
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        return new HorizontalLayout(edit, delete);
    }

    private void refreshAccounts() {
        accountGrid.setItems(
                query -> {
                    Pageable pageable = AdminGridUtil.pageable(query, "createdAt");
                    Page<Account> page = adminPanelService.findAccountsByUser(userId, accountSearch.getValue(), pageable);
                    return page.stream();
                },
                query -> {
                    Pageable pageable = AdminGridUtil.pageable(query, "createdAt");
                    return (int) adminPanelService.findAccountsByUser(userId, accountSearch.getValue(), pageable)
                            .getTotalElements();
                });
    }

    private void openAccountDialog(Account editing) {
        boolean editMode = editing != null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(editMode ? "Edit account" : "Create account");
        dialog.setWidth("520px");

        TextField name = new TextField("Name");
        name.setWidthFull();

        ComboBox<AccountType> type = new ComboBox<>("Type");
        type.setItems(AccountType.values());
        type.setWidthFull();

        ComboBox<Currency> currency = new ComboBox<>("Currency");
        currency.setItems(Currency.values());
        currency.setWidthFull();

        TextField initialBalance = new TextField("Initial Balance");
        initialBalance.setValue("0");
        initialBalance.setWidthFull();

        TextField cardNumber = new TextField("Card Number");
        cardNumber.setWidthFull();

        ComboBox<CardType> cardType = new ComboBox<>("Card Type");
        cardType.setItems(CardType.values());
        cardType.setWidthFull();

        TextField expiryDate = new TextField("Expiry Date (MM/YY)");
        expiryDate.setWidthFull();

        if (editMode) {
            name.setValue(editing.getName());
            type.setValue(editing.getType());
            type.setEnabled(false);
            currency.setValue(editing.getCurrency());
            initialBalance.setValue(editing.getBalance().toPlainString());
            initialBalance.setEnabled(false);
            cardNumber.setValue(editing.getCardNumber() == null ? "" : editing.getCardNumber());
            cardType.setValue(editing.getCardType());
            expiryDate.setValue(editing.getExpiryDate() == null ? "" : editing.getExpiryDate());
        }

        VerticalLayout body = new VerticalLayout(name, type, currency, initialBalance, cardNumber, cardType, expiryDate);
        body.setPadding(false);

        Button save = new Button("Save", e -> {
            if (name.getValue().isBlank() || type.getValue() == null || currency.getValue() == null) {
                Notification.show("Name, type and currency are required", 2500, Notification.Position.TOP_END);
                return;
            }

            AdminAccountForm form = new AdminAccountForm();
            form.setName(name.getValue().trim());
            form.setType(type.getValue());
            form.setCurrency(currency.getValue());
            form.setCardNumber(cardNumber.getValue().isBlank() ? null : cardNumber.getValue().trim());
            form.setCardType(cardType.getValue());
            form.setExpiryDate(expiryDate.getValue().isBlank() ? null : expiryDate.getValue().trim());

            try {
                form.setInitialBalance(new BigDecimal(initialBalance.getValue().trim()));
            } catch (Exception ex) {
                form.setInitialBalance(BigDecimal.ZERO);
            }

            try {
                if (editMode) {
                    adminPanelService.updateAccountForUser(userId, editing.getId(), form);
                    Notification.show("Account updated", 2000, Notification.Position.TOP_END);
                } else {
                    adminPanelService.createAccountForUser(userId, form);
                    Notification.show("Account created", 2000, Notification.Position.TOP_END);
                }
                dialog.close();
                refreshAccounts();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.TOP_END);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.add(body);
        dialog.open();
    }
}
