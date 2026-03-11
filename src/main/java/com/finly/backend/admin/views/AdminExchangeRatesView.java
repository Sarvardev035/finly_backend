package com.finly.backend.admin.views;

import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.Currency;
import com.finly.backend.domain.model.ExchangeRateSource;
import com.finly.backend.dto.request.ExchangeRateUpsertRequest;
import com.finly.backend.dto.response.ExchangeRateResponse;
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
import com.vaadin.flow.server.Command;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Route(value = "admin/exchange-rates", layout = AdminMainLayout.class)
@PageTitle("Exchange Rates | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminExchangeRatesView extends VerticalLayout {

    private final AdminApiClient apiClient;
    private final Grid<ExchangeRateResponse> grid = new Grid<>(ExchangeRateResponse.class, false);

    public AdminExchangeRatesView(AdminApiClient apiClient) {
        this.apiClient = apiClient;
        addClassName("admin-page");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Exchange Rates"));
        add(buildToolbar(), buildGrid());
        refreshList();
    }

    private HorizontalLayout buildToolbar() {
        Button refresh = new Button("Refresh from Internet");
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refresh.addClickListener(e -> refreshFromInternet());

        Button add = new Button("Add Override");
        add.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        add.addClickListener(e -> openForm(null));

        HorizontalLayout toolbar = new HorizontalLayout(refresh, add);
        toolbar.setWidthFull();
        return toolbar;
    }

    private Grid<ExchangeRateResponse> buildGrid() {
        grid.setSizeFull();
        grid.addColumn(r -> r.getBaseCurrency() == null ? "-" : r.getBaseCurrency().name()).setHeader("Base")
                .setSortable(true);
        grid.addColumn(r -> r.getTargetCurrency() == null ? "-" : r.getTargetCurrency().name()).setHeader("Target")
                .setSortable(true);
        grid.addColumn(ExchangeRateResponse::getRate).setHeader("Rate").setSortable(true);
        grid.addColumn(r -> r.getSource() == null ? "-" : r.getSource().name()).setHeader("Source");
        grid.addColumn(r -> r.getCreatedAt() == null ? "-" : r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setHeader("Created At").setSortable(true);
        grid.addComponentColumn(item -> {
            Button edit = new Button("Edit", e -> openForm(item));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return new HorizontalLayout(edit);
        }).setHeader("Actions");
        return grid;
    }

    private void refreshList() {
        UI ui = UI.getCurrent();
        apiClient.getArray("/api/exchange-rates", ExchangeRateResponse[].class)
                .subscribe(items -> runOnUi(ui, () -> {
                    List<ExchangeRateResponse> list = items == null ? List.of() : Arrays.asList(items);
                    grid.setItems(list);
                }), err -> runOnUi(ui, () -> Notification.show("Failed to load rates: " + err.getMessage(), 4000,
                        Notification.Position.TOP_END)));
    }

    private void refreshFromInternet() {
        UI ui = UI.getCurrent();
        apiClient.post("/api/exchange-rates/refresh", Map.of(), ExchangeRateResponse[].class)
                .subscribe(resp -> runOnUi(ui, () -> {
                    Notification.show("Rates refreshed from Internet", 2000, Notification.Position.TOP_END);
                    refreshList();
                }), err -> runOnUi(ui, () -> Notification.show("Refresh failed: " + err.getMessage(), 4000,
                        Notification.Position.TOP_END)));
    }

    private void openForm(ExchangeRateResponse editing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(editing == null ? "Add Exchange Rate Override" : "Edit Exchange Rate Override");
        dialog.setWidth("420px");

        ComboBox<Currency> base = new ComboBox<>("Base Currency");
        base.setItems(Currency.values());
        base.setWidthFull();

        ComboBox<Currency> target = new ComboBox<>("Target Currency");
        target.setItems(Currency.values());
        target.setWidthFull();

        BigDecimalField rate = new BigDecimalField("Rate");
        rate.setWidthFull();

        if (editing != null) {
            base.setValue(editing.getBaseCurrency());
            target.setValue(editing.getTargetCurrency());
            rate.setValue(editing.getRate());
        }

        VerticalLayout body = new VerticalLayout(base, target, rate);
        body.setPadding(false);
        body.setSpacing(true);
        dialog.add(body);

        Button cancel = new Button("Cancel", e -> dialog.close());
        Button save = new Button("Save", e -> {
            if (base.getValue() == null || target.getValue() == null || rate.getValue() == null) {
                Notification.show("Base, target and rate are required", 2500, Notification.Position.TOP_END);
                return;
            }
            if (base.getValue() == target.getValue()) {
                Notification.show("Base and target cannot be the same", 2500, Notification.Position.TOP_END);
                return;
            }
            ExchangeRateUpsertRequest req = ExchangeRateUpsertRequest.builder()
                    .baseCurrency(base.getValue())
                    .targetCurrency(target.getValue())
                    .rate(rate.getValue())
                    .build();
            saveRate(dialog, editing, req);
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void saveRate(Dialog dialog, ExchangeRateResponse editing, ExchangeRateUpsertRequest req) {
        UI ui = UI.getCurrent();
        if (editing == null || editing.getId() == null || editing.getSource() != ExchangeRateSource.ADMIN) {
            apiClient.post("/api/exchange-rates", req, ExchangeRateResponse.class)
                    .subscribe(saved -> runOnUi(ui, () -> {
                        dialog.close();
                        Notification.show("Override saved", 2000, Notification.Position.TOP_END);
                        refreshList();
                    }), err -> runOnUi(ui, () -> Notification.show("Save failed: " + err.getMessage(), 4000,
                            Notification.Position.TOP_END)));
            return;
        }

        apiClient.put("/api/exchange-rates/" + editing.getId(), req, ExchangeRateResponse.class)
                .subscribe(updated -> runOnUi(ui, () -> {
                    dialog.close();
                    Notification.show("Override updated", 2000, Notification.Position.TOP_END);
                    refreshList();
                }), err -> runOnUi(ui, () -> Notification.show("Update failed: " + err.getMessage(), 4000,
                        Notification.Position.TOP_END)));
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
