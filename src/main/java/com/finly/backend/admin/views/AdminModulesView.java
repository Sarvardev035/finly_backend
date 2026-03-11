package com.finly.backend.admin.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/modules", layout = AdminMainLayout.class)
@PageTitle("Module Management | Finly")
@RolesAllowed("ADMIN")
public class AdminModulesView extends VerticalLayout {

    public AdminModulesView() {
        addClassName("admin-page");
        add(new Div(new Span("Admin"), new Span(" / "), new Span("Module Management")));

        H3 title = new H3("Module Management");
        Div info = new Div();
        info.setText("Use User Workspace from /admin/users/{id} to manage accounts, categories, expenses, incomes, transfers, debts, budgets, analytics, and notifications by user scope.");

        add(title, info);
    }
}
