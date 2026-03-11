package com.finly.backend.admin.views;

import com.finly.backend.admin.services.AdminApiClient;
import com.finly.backend.dto.response.ApiResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.Map;

@Route(value = "admin/system", layout = AdminMainLayout.class)
@PageTitle("System API Tester | Finly Admin")
@RolesAllowed("ADMIN")
public class AdminSystemView extends VerticalLayout {

    private final AdminApiClient apiClient;

    public AdminSystemView(AdminApiClient apiClient) {
        this.apiClient = apiClient;
        addClassName("admin-page");
        setSpacing(true);
        setPadding(true);

        add(new H2("System API Endpoint Tester"));

        VerticalLayout card = new VerticalLayout();
        card.addClassName("admin-card");

        FormLayout form = new FormLayout();

        TextField endpointField = new TextField("Endpoint");
        endpointField.setPlaceholder("e.g. data-sync, health-check");
        endpointField.setRequired(true);

        TextField methodField = new TextField("Method");
        methodField.setPlaceholder("e.g. initialize, refresh");
        methodField.setRequired(true);

        TextArea bodyField = new TextArea("Payload (JSON)");
        bodyField.setPlaceholder("{\"key\": \"value\"}");
        bodyField.setWidthFull();

        form.add(endpointField, methodField, bodyField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button sendBtn = new Button("Send Request", e -> {
            try {
                sendRequest(endpointField.getValue(), methodField.getValue(), bodyField.getValue());
            } catch (Exception ex) {
                Notification.show("Error building request: " + ex.getMessage());
            }
        });
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Pre resultArea = new Pre();
        resultArea.getStyle().set("background", "#f0f0f0");
        resultArea.getStyle().set("padding", "1rem");
        resultArea.getStyle().set("min-height", "100px");
        resultArea.getStyle().set("width", "100%");
        resultArea.setText("// Response will appear here");

        card.add(form, sendBtn, resultArea);
        add(card);
    }

    private void sendRequest(String endpoint, String method, String body) {
        String path = "/api/connect/" + endpoint + "/" + method.toUpperCase();
        // In a real implementation, we'd parse the JSON body
        Map<String, Object> payload = Map.of("raw", body);

        apiClient.post(path, payload, Map.class)
                .subscribe(response -> {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        Notification.show("Request successful");
                    }));
                }, error -> {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        Notification.show("Request failed: " + error.getMessage());
                    }));
                });
    }
}
