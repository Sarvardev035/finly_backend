package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("admin/login")
@PageTitle("Admin Login | Finly")
@AnonymousAllowed
public class AdminLoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();
    private final VaadinSecurityContext securityContext;

    public AdminLoginView(VaadinSecurityContext securityContext) {
        this.securityContext = securityContext;

        addClassName("admin-login-page");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Div card = new Div();
        card.addClassName("admin-card");
        card.getStyle().set("width", "420px");
        card.getStyle().set("padding", "2rem");

        H1 logo = new H1("Finly");
        logo.getStyle().set("color", "#1e3a8a");
        logo.getStyle().set("margin", "0");

        H2 heading = new H2("Admin Panel");
        heading.getStyle().set("margin", "0.5rem 0");

        Paragraph subtitle = new Paragraph("Sign in with your administrator account");
        subtitle.getStyle().set("margin-top", "0");

        loginForm.setAction("/admin/login");
        loginForm.setForgotPasswordButtonVisible(false);

        card.add(logo, heading, subtitle, loginForm);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (securityContext.isAdminAuthenticated()) {
            event.forwardTo("admin/dashboard");
            return;
        }

        boolean hasError = event.getLocation().getQueryParameters().getParameters().containsKey("error");
        loginForm.setError(hasError);
    }
}
