package com.finly.backend.admin.views;

import com.finly.backend.admin.dto.AdminUserForm;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.domain.model.Role;
import com.finly.backend.domain.model.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.format.DateTimeFormatter;

@Route(value = "admin/users", layout = AdminMainLayout.class)
@PageTitle("Users | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminUsersView extends VerticalLayout {

    private final AdminPanelService adminPanelService;
    private final Grid<User> grid = new Grid<>(User.class, false);
    private final TextField searchField = new TextField();

    public AdminUsersView(AdminPanelService adminPanelService) {
        this.adminPanelService = adminPanelService;
        addClassName("admin-page");
        setSizeFull();

        add(breadcrumb(), header(), buildGrid());
        refreshGrid();
    }

    private Div breadcrumb() {
        Div bread = new Div(new Span("Admin"), new Span(" / "), new Span("Users"));
        bread.addClassName("admin-breadcrumb");
        return bread;
    }

    private HorizontalLayout header() {
        H3 title = new H3("User Management");

        searchField.setPlaceholder("Search by full name or email");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> refreshGrid());
        searchField.addClassName("admin-search");

        Button addUser = new Button("Add User", e -> openUserDialog(null));
        addUser.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout row = new HorizontalLayout(title, searchField, addUser);
        row.setWidthFull();
        row.expand(title);
        row.setAlignItems(Alignment.CENTER);
        return row;
    }

    private Grid<User> buildGrid() {
        grid.addColumn(User::getFullName).setHeader("Full Name").setKey("fullName").setSortable(true)
                .setAutoWidth(true);
        grid.addColumn(User::getEmail).setHeader("Email").setKey("email").setSortable(true).setAutoWidth(true);
        grid.addColumn(user -> user.getRole().name()).setHeader("Role").setKey("role").setSortable(true);
        grid.addColumn(new ComponentRenderer<>(user -> statusBadge()))
                .setHeader("Status")
                .setAutoWidth(true);
        grid.addColumn(user -> user.getCreatedAt() == null ? "-"
                : user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setHeader("Created")
                .setKey("createdAt")
                .setSortable(true)
                .setAutoWidth(true);
        grid.addColumn(new ComponentRenderer<>(this::rowActions)).setHeader("Actions").setAutoWidth(true);

        grid.setSizeFull();
        grid.addClassName("admin-grid");
        grid.setPageSize(20);
        grid.sort(GridSortOrder.asc(grid.getColumnByKey("fullName")).build());
        return grid;
    }

    private HorizontalLayout rowActions(User user) {
        Button view = new Button("Open", e -> getUI().ifPresent(ui -> ui.navigate("admin/users/" + user.getId())));
        Button edit = new Button("Edit", e -> openUserDialog(user));
        Button delete = new Button("Delete", e -> {
            adminPanelService.deleteUser(user.getId());
            Notification.show("User deleted", 2500, Notification.Position.TOP_END);
            refreshGrid();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout actions = new HorizontalLayout(view, edit, delete);
        actions.setSpacing(false);
        return actions;
    }

    private Span statusBadge() {
        Span badge = new Span("ACTIVE");
        badge.getElement().getThemeList().add("badge success");
        return badge;
    }

    private void refreshGrid() {
        grid.setItems(
                query -> {
                    Pageable pageable = AdminGridUtil.pageable(query, "createdAt");
                    Page<User> page = adminPanelService.findUsers(searchField.getValue(), pageable);
                    return page.stream();
                },
                query -> {
                    Pageable pageable = AdminGridUtil.pageable(query, "createdAt");
                    return (int) adminPanelService.findUsers(searchField.getValue(), pageable).getTotalElements();
                });
    }

    private void openUserDialog(User editingUser) {
        boolean editMode = editingUser != null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(editMode ? "Edit user" : "Create user");
        dialog.setWidth("480px");

        AdminUserForm form = new AdminUserForm();
        if (editMode) {
            form.setFullName(editingUser.getFullName());
            form.setEmail(editingUser.getEmail());
            form.setRole(editingUser.getRole());
        } else {
            form.setRole(Role.USER);
        }

        TextField fullName = new TextField("Full Name");
        fullName.setWidthFull();
        fullName.setRequired(true);

        EmailField email = new EmailField("Email");
        email.setWidthFull();
        email.setRequiredIndicatorVisible(true);

        PasswordField password = new PasswordField(editMode ? "New Password (optional)" : "Password");
        password.setWidthFull();
        password.setRequired(!editMode);

        ComboBox<Role> role = new ComboBox<>("Role");
        role.setItems(Role.values());
        role.setWidthFull();

        fullName.setValue(form.getFullName() == null ? "" : form.getFullName());
        email.setValue(form.getEmail() == null ? "" : form.getEmail());
        role.setValue(form.getRole());

        VerticalLayout formLayout = new VerticalLayout(fullName, email, password, role);
        formLayout.setPadding(false);

        Button save = new Button("Save", e -> {
            if (fullName.getValue().isBlank() || email.getValue().isBlank() || role.getValue() == null) {
                Notification.show("Full name, email and role are required", 2500, Notification.Position.TOP_END);
                return;
            }
            if (!editMode && password.getValue().length() < 6) {
                Notification.show("Password must be at least 6 characters", 2500, Notification.Position.TOP_END);
                return;
            }

            form.setFullName(fullName.getValue().trim());
            form.setEmail(email.getValue().trim());
            form.setRole(role.getValue());
            form.setPassword(password.getValue());

            try {
                if (editMode) {
                    adminPanelService.updateUser(editingUser.getId(), form);
                    Notification.show("User updated", 2500, Notification.Position.TOP_END);
                } else {
                    adminPanelService.createUser(form);
                    Notification.show("User created", 2500, Notification.Position.TOP_END);
                }
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.TOP_END);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancel, save);
        dialog.add(formLayout);
        dialog.open();
    }
}
