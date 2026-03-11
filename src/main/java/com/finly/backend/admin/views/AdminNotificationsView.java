package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.Notification;
import com.finly.backend.domain.model.NotificationType;
import com.finly.backend.domain.model.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Route(value = "admin/notifications", layout = AdminMainLayout.class)
@PageTitle("Notifications | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminNotificationsView extends VerticalLayout {

    private final AdminApiClient apiClient;
    private final AdminPanelService adminPanelService;
    private final VaadinSecurityContext securityContext;

    private ComboBox<User> userSelector;
    private ComboBox<NotificationType> typeFilter;
    private ComboBox<String> statusFilter;
    private TextField searchFilter;
    private Grid<Notification> grid;
    private List<Notification> allNotifications = new ArrayList<>();

    public AdminNotificationsView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        this.apiClient = apiClient;
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;

        addClassName("admin-page");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Notifications"), buildToolbar(), buildGrid());
        preloadUsers();
    }

    private HorizontalLayout buildToolbar() {
        userSelector = new ComboBox<>("Select User");
        userSelector.setWidth("420px");
        userSelector.setItemLabelGenerator(user -> {
            String name = user.getFullName() == null ? "No Name" : user.getFullName();
            return name + " (" + user.getEmail() + ")";
        });
        userSelector.addValueChangeListener(event -> {
            User selected = event.getValue();
            securityContext.setActAsUserId(selected == null ? null : selected.getId().toString());
            loadNotifications();
        });

        typeFilter = new ComboBox<>("Type");
        typeFilter.setItems(NotificationType.values());
        typeFilter.setClearButtonVisible(true);
        typeFilter.addValueChangeListener(event -> applyFilter());

        statusFilter = new ComboBox<>("Status");
        statusFilter.setItems("ALL", "UNREAD", "READ");
        statusFilter.setValue("ALL");
        statusFilter.addValueChangeListener(event -> applyFilter());

        searchFilter = new TextField("Search");
        searchFilter.setPlaceholder("Message...");
        searchFilter.setClearButtonVisible(true);
        searchFilter.addValueChangeListener(event -> applyFilter());

        Button refresh = new Button("Refresh", e -> loadNotifications());
        refresh.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout bar = new HorizontalLayout(userSelector, typeFilter, statusFilter, searchFilter, refresh);
        bar.setWidthFull();
        bar.setAlignItems(Alignment.END);
        return bar;
    }

    private Grid<Notification> buildGrid() {
        grid = new Grid<>(Notification.class, false);
        grid.setSizeFull();
        grid.addColumn(n -> n.getId() == null ? "-" : n.getId().toString()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(n -> n.getType() == null ? "-" : n.getType().name()).setHeader("Type");
        grid.addColumn(Notification::getMessage).setHeader("Message").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(n -> n.getIsRead() != null && n.getIsRead() ? "READ" : "UNREAD").setHeader("Status");
        grid.addColumn(n -> n.getCreatedAt() == null ? "-" : n.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setHeader("Created");
        grid.addComponentColumn(item -> {
            Button markRead = new Button("Mark as read", e -> markAsRead(item));
            markRead.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_TERTIARY);
            markRead.setEnabled(item.getIsRead() == null || !item.getIsRead());
            return markRead;
        }).setHeader("Action");
        return grid;
    }

    private void preloadUsers() {
        var users = adminPanelService.findUsers("", PageRequest.of(0, 200)).getContent();
        userSelector.setItems(users);
        if (!users.isEmpty()) {
            userSelector.setValue(users.get(0));
        } else {
            com.vaadin.flow.component.notification.Notification.show("No users found", 2000, Position.TOP_END);
        }
    }

    private void loadNotifications() {
        if (userSelector.getValue() == null) {
            allNotifications = new ArrayList<>();
            grid.setItems(List.of());
            return;
        }
        UI ui = UI.getCurrent();
        apiClient.getArray("/api/notifications/all", Notification[].class)
                .subscribe(items -> runOnUi(ui, () -> {
                    allNotifications = items == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(items));
                    applyFilter();
                }), err -> runOnUi(ui, () -> com.vaadin.flow.component.notification.Notification.show(
                        "Failed to load notifications: " + cleanError(err), 4000, Position.TOP_END)));
    }

    private void applyFilter() {
        NotificationType type = typeFilter.getValue();
        String status = statusFilter.getValue();
        String search = searchFilter.getValue();
        String normalized = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        grid.setItems(allNotifications.stream()
                .filter(n -> type == null || type == n.getType())
                .filter(n -> "ALL".equals(status)
                        || ("READ".equals(status) && Boolean.TRUE.equals(n.getIsRead()))
                        || ("UNREAD".equals(status) && !Boolean.TRUE.equals(n.getIsRead())))
                .filter(n -> normalized.isBlank()
                        || (n.getMessage() != null && n.getMessage().toLowerCase(Locale.ROOT).contains(normalized)))
                .toList());
    }

    private void markAsRead(Notification notification) {
        if (notification == null || notification.getId() == null) {
            com.vaadin.flow.component.notification.Notification.show("Invalid notification", 2000, Position.TOP_END);
            return;
        }
        UI ui = UI.getCurrent();
        apiClient.post("/api/notifications/" + notification.getId() + "/read", java.util.Map.of(), Void.class)
                .subscribe(resp -> runOnUi(ui, () -> {
                    notification.setIsRead(true);
                    applyFilter();
                    com.vaadin.flow.component.notification.Notification.show("Marked as read", 2000, Position.TOP_END);
                }), err -> runOnUi(ui, () -> com.vaadin.flow.component.notification.Notification.show(
                        "Failed to mark as read: " + cleanError(err), 4000, Position.TOP_END)));
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
