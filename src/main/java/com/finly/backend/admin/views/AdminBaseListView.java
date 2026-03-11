package com.finly.backend.admin.views;

import com.finly.backend.admin.services.AdminApiClient;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class AdminBaseListView<T> extends VerticalLayout {

    protected final AdminApiClient apiClient;
    protected final Grid<T> grid;
    protected final TextField filterText = new TextField();
    protected final Dialog formDialog = new Dialog();
    protected final FormLayout form = new FormLayout();
    protected final Span countLabel = new Span("Items: 0");
    private final Span pageInfoLabel = new Span("Page 1 of 1");
    private final Button prevPageBtn = new Button(VaadinIcon.CHEVRON_LEFT.create());
    private final Button nextPageBtn = new Button(VaadinIcon.CHEVRON_RIGHT.create());

    private final Class<T> entityClass;
    private final String apiPath;
    private final int pageSize = 15;
    private int currentPage = 0;
    private List<T> allItems = new ArrayList<>();

    public AdminBaseListView(AdminApiClient apiClient, Class<T> entityClass, String apiPath, String title) {
        this.apiClient = apiClient;
        this.entityClass = entityClass;
        this.apiPath = apiPath;
        this.grid = new Grid<>(entityClass);

        addClassName("admin-page");
        setSizeFull();

        add(new H2(title), getToolbar(), getContent());
        configureGrid();
        configureForm();

        updateList();
    }

    private Component getToolbar() {
        filterText.setPlaceholder("Search...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> {
            currentPage = 0;
            refreshGridPage();
        });
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());

        Button addBtn = createAddButton();

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addBtn);
        toolbar.addClassName("admin-toolbar");
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return toolbar;
    }

    private Component getContent() {
        VerticalLayout container = new VerticalLayout(grid);
        container.setPadding(false);
        container.setSpacing(false);
        container.setSizeFull();

        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setPadding(true);
        footer.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footer.setAlignItems(Alignment.CENTER);

        HorizontalLayout pagination = new HorizontalLayout();
        prevPageBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextPageBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevPageBtn.addClickListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshGridPage();
            }
        });
        nextPageBtn.addClickListener(e -> {
            int totalPages = totalPages(filteredItems().size());
            if (currentPage < totalPages - 1) {
                currentPage++;
                refreshGridPage();
            }
        });
        pagination.add(prevPageBtn, pageInfoLabel, nextPageBtn);
        pagination.setAlignItems(Alignment.CENTER);

        footer.add(countLabel, pagination);
        container.add(footer);

        return container;
    }

    protected void configureGrid() {
        grid.addClassName("admin-grid");
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addItemDoubleClickListener(e -> editEntity(e.getItem()));

        grid.addComponentColumn(item -> {
            Button edit = new Button(VaadinIcon.EDIT.create(), e -> editEntity(item));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> deleteEntity(item));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            return new HorizontalLayout(edit, delete);
        }).setHeader("Actions").setFrozenToEnd(true).setWidth("120px").setFlexGrow(0);
    }

    private void configureForm() {
        formDialog.setHeaderTitle("Entity Details");
        formDialog.add(form);
        formDialog.setWidth("500px");

        Button saveBtn = new Button("Save", e -> saveEntity());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> formDialog.close());

        formDialog.getFooter().add(cancelBtn, saveBtn);
    }

    protected abstract void buildForm(T entity);

    protected abstract T getFormEntity();

    protected void updateList() {
        UI ui = UI.getCurrent();
        apiClient.getArray(apiPath, arrayClass(entityClass))
                .subscribe(array -> runOnUi(ui, () -> {
                    List<T> list = array == null ? List.of() : Arrays.asList(array);
                    allItems = new ArrayList<>(list);
                    currentPage = 0;
                    refreshGridPage();
                }), error -> runOnUi(ui, () -> logError("Failed to fetch data", error)));
    }

    protected Button createAddButton() {
        Button addBtn = new Button("Create New", VaadinIcon.PLUS.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.addClickListener(e -> addEntity());
        return addBtn;
    }

    protected void addEntity() {
        try {
            T newEntity = entityClass.getDeclaredConstructor().newInstance();
            showForm(newEntity);
        } catch (Exception e) {
            logError("Failed to create new instance", e);
        }
    }

    protected void editEntity(T entity) {
        showForm(entity);
    }

    private void showForm(T entity) {
        form.removeAll();
        buildForm(entity);
        formDialog.open();
    }

    private void saveEntity() {
        T entity = getFormEntity();
        handleSave(entity);
    }

    protected void handleSave(T entity) {
        String id = safeEntityId(entity);
        try {
            if (id == null || id.isBlank()) {
                apiClient.post(apiPath, entity, entityClass).block();
            } else {
                apiClient.put(apiPath + "/" + id, entity, entityClass).block();
            }
            onSaveSuccess();
        } catch (Exception error) {
            logError(id == null || id.isBlank() ? "Create failed" : "Update failed", error);
        }
    }

    protected void deleteEntity(T entity) {
        try {
            apiClient.delete(apiPath + "/" + getEntityId(entity)).block();
            Notification.show("Deleted successfully");
            updateList();
        } catch (Exception error) {
            logError("Delete failed", error);
        }
    }

    protected abstract String getEntityId(T entity);

    protected void onSaveSuccess() {
        Notification.show("Saved successfully");
        formDialog.close();
        updateList();
    }

    private String safeEntityId(T entity) {
        try {
            return getEntityId(entity);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void refreshGridPage() {
        List<T> filtered = filteredItems();
        int totalPages = totalPages(filtered.size());
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }

        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, filtered.size());
        List<T> pageItems = from >= to ? List.of() : filtered.subList(from, to);

        grid.setItems(pageItems);
        countLabel.setText("Total items: " + filtered.size());
        pageInfoLabel.setText("Page " + (totalPages == 0 ? 0 : currentPage + 1) + " of " + totalPages);
        prevPageBtn.setEnabled(currentPage > 0);
        nextPageBtn.setEnabled(currentPage < totalPages - 1);
    }

    private List<T> filteredItems() {
        String q = filterText.getValue();
        if (q == null || q.isBlank()) {
            return allItems;
        }
        String query = q.toLowerCase(Locale.ROOT);
        return allItems.stream().filter(item -> matches(item, query)).toList();
    }

    private boolean matches(T item, String query) {
        try {
            for (Method method : item.getClass().getMethods()) {
                if (method.getParameterCount() == 0
                        && method.getName().startsWith("get")
                        && !method.getName().equals("getClass")) {
                    Object value = method.invoke(item);
                    if (value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(query)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
            return Objects.toString(item, "").toLowerCase(Locale.ROOT).contains(query);
        }
        return false;
    }

    private int totalPages(int size) {
        int pages = (int) Math.ceil(size / (double) pageSize);
        return Math.max(1, pages);
    }

    @SuppressWarnings("unchecked")
    private Class<T[]> arrayClass(Class<T> componentType) {
        return (Class<T[]>) Array.newInstance(componentType, 0).getClass();
    }

    private void logError(String msg, Throwable error) {
        Notification n = Notification.show(msg + ": " + error.getMessage());
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void runOnUi(UI ui, Command action) {
        if (action == null) {
            return;
        }
        if (ui == null || ui.getSession() == null) {
            return;
        }
        ui.access(action);
    }
}
