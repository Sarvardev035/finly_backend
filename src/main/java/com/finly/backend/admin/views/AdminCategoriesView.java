package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.finly.backend.admin.service.AdminPanelService;
import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.domain.model.CategoryType;
import com.finly.backend.domain.model.User;
import com.finly.backend.dto.request.CreateCategoryRequest;
import com.finly.backend.dto.request.UpdateCategoryRequest;
import com.finly.backend.dto.response.CategoryResponse;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Route(value = "admin/categories", layout = AdminMainLayout.class)
@PageTitle("Categories | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminCategoriesView extends AdminBaseListView<CategoryResponse> {

    private final AdminPanelService adminPanelService;
    private final VaadinSecurityContext securityContext;
    private ComboBox<User> userSelector;
    private User selectedUser;

    private TextField nameField;
    private ComboBox<CategoryType> typeField;
    private CategoryResponse currentCategory;

    public AdminCategoriesView(AdminApiClient apiClient, AdminPanelService adminPanelService,
            VaadinSecurityContext securityContext) {
        super(apiClient, CategoryResponse.class, "/api/categories", "Category Management");
        this.adminPanelService = adminPanelService;
        this.securityContext = securityContext;
        addComponentAtIndex(1, buildUserSelectorBar());
        preloadUsers();
    }

    @Override
    protected void configureGrid() {
        super.configureGrid();
        grid.removeAllColumns();
        grid.addColumn(c -> c.getId() == null ? "-" : c.getId().toString()).setHeader("ID").setAutoWidth(true);
        grid.addColumn(CategoryResponse::getName).setHeader("Name").setSortable(true);
        grid.addColumn(c -> c.getType() == null ? "-" : c.getType().name()).setHeader("Type").setSortable(true);
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
        Button add = new Button("Add Category");
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
    protected void buildForm(CategoryResponse category) {
        currentCategory = cloneCategory(category);

        nameField = new TextField("Category Name");
        nameField.setValue(currentCategory.getName() == null ? "" : currentCategory.getName());

        typeField = new ComboBox<>("Type");
        typeField.setItems(CategoryType.values());
        typeField.setValue(currentCategory.getType() == null ? CategoryType.EXPENSE : currentCategory.getType());

        form.add(nameField, typeField);
    }

    @Override
    protected CategoryResponse getFormEntity() {
        currentCategory.setName(nameField.getValue());
        currentCategory.setType(typeField.getValue());
        return currentCategory;
    }

    @Override
    protected String getEntityId(CategoryResponse entity) {
        return entity == null || entity.getId() == null ? null : entity.getId().toString();
    }

    @Override
    protected void handleSave(CategoryResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        if (entity.getId() == null) {
            CreateCategoryRequest request = CreateCategoryRequest.builder()
                    .name(entity.getName())
                    .type(entity.getType())
                    .build();
            apiClient.post("/api/categories", request, CategoryResponse.class)
                    .subscribe(saved -> runOnUi(ui, () -> {
                        Notification.show("Category successfully added", 2000, Notification.Position.TOP_END);
                        formDialog.close();
                        updateList();
                    }), err -> runOnUi(ui, () -> Notification.show("Create failed: " + cleanErrorMessage(err), 4000,
                            Notification.Position.TOP_END)));
            return;
        }

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name(entity.getName())
                .type(entity.getType())
                .build();
        apiClient.put("/api/categories/" + entity.getId(), request, CategoryResponse.class)
                .subscribe(updated -> runOnUi(ui, () -> {
                    Notification.show("Category successfully updated", 2000, Notification.Position.TOP_END);
                    formDialog.close();
                    updateList();
                }), err -> runOnUi(ui, () -> Notification.show("Update failed: " + cleanErrorMessage(err), 4000,
                        Notification.Position.TOP_END)));
    }

    @Override
    protected void deleteEntity(CategoryResponse entity) {
        if (selectedUser == null) {
            Notification.show("Select a user first", 2000, Notification.Position.TOP_END);
            return;
        }
        if (entity == null || entity.getId() == null) {
            Notification.show("Invalid category id", 2000, Notification.Position.TOP_END);
            return;
        }

        UI ui = UI.getCurrent();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Delete category?");
        confirm.add("Category \"" + entity.getName() + "\" will be permanently deleted.");

        Button cancel = new Button("Cancel", e -> confirm.close());
        Button yesDelete = new Button("Delete", e -> apiClient.delete("/api/categories/" + entity.getId())
                .subscribe(v -> {
                }, err -> runOnUi(ui, () -> Notification.show("Delete failed: " + cleanErrorMessage(err), 4000,
                        Notification.Position.TOP_END)), () -> runOnUi(ui, () -> {
                            confirm.close();
                            Notification.show("Category successfully deleted", 2000, Notification.Position.TOP_END);
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

    private CategoryResponse cloneCategory(CategoryResponse source) {
        if (source == null) {
            return new CategoryResponse();
        }
        return CategoryResponse.builder()
                .id(source.getId())
                .name(source.getName())
                .type(source.getType())
                .createdAt(source.getCreatedAt())
                .build();
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
