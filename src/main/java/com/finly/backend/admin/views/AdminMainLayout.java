package com.finly.backend.admin.views;

import com.finly.backend.admin.security.VaadinSecurityContext;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("ADMIN")
public class AdminMainLayout extends AppLayout implements BeforeEnterObserver {

    private final VaadinSecurityContext securityContext;

    public AdminMainLayout(VaadinSecurityContext securityContext) {
        this.securityContext = securityContext;
        addClassName("admin-shell");
        createHeader();
        createDrawer();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!securityContext.isAdminAuthenticated()) {
            event.rerouteTo(AdminLoginView.class);
        }
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.addClassName("admin-toggle");

        H2 title = new H2("Finly Admin");
        title.addClassName("admin-title");

        TextField search = new TextField();
        search.setPlaceholder("Global search...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.addClassName("admin-global-search");

        Span userLabel = new Span(securityContext.getCurrentUserEmail());
        userLabel.addClassName("admin-user-label");

        Button notificationBtn = new Button(VaadinIcon.BELL.create());
        notificationBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        notificationBtn.addClassName("admin-nav-btn");

        Button logout = new Button("Logout", e -> {
            securityContext.clearToken();
            getUI().ifPresent(ui -> ui.getPage().setLocation("/admin/logout"));
        });
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logout.addClassName("admin-logout");

        HorizontalLayout header = new HorizontalLayout(toggle, title, spacer(), search, userLabel, notificationBtn,
                logout);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassName("admin-header");
        addToNavbar(header);
    }

    private void createDrawer() {
        Span logo = new Span("Finly Backoffice");
        logo.addClassName("admin-logo");

        SideNav sideNav = new SideNav();
        sideNav.addItem(new SideNavItem("Dashboard", "/admin/dashboard", VaadinIcon.DASHBOARD.create()));
        sideNav.addItem(new SideNavItem("Users", "/admin/users", VaadinIcon.USERS.create()));
        sideNav.addItem(new SideNavItem("Accounts", "/admin/accounts", VaadinIcon.WALLET.create()));
        sideNav.addItem(new SideNavItem("Income", "/admin/incomes", VaadinIcon.ARROW_CIRCLE_UP_O.create()));
        sideNav.addItem(new SideNavItem("Expenses", "/admin/expenses", VaadinIcon.ARROW_CIRCLE_DOWN_O.create()));
        sideNav.addItem(new SideNavItem("Transfers", "/admin/transfers", VaadinIcon.EXCHANGE.create()));
        sideNav.addItem(new SideNavItem("Budgets", "/admin/budgets", VaadinIcon.CHART_LINE.create()));
        sideNav.addItem(new SideNavItem("Debts", "/admin/debts", VaadinIcon.FROWN_O.create()));
        sideNav.addItem(new SideNavItem("Categories", "/admin/categories", VaadinIcon.LIST.create()));
        sideNav.addItem(new SideNavItem("Exchange Rates", "/admin/exchange-rates", VaadinIcon.EXCHANGE.create()));
        sideNav.addItem(new SideNavItem("Analytics", "/admin/analytics", VaadinIcon.BAR_CHART.create()));
        sideNav.addItem(new SideNavItem("Notifications", "/admin/notifications", VaadinIcon.BELL.create()));
        sideNav.addItem(new SideNavItem("System", "/admin/system", VaadinIcon.COG.create()));
        sideNav.addClassName("admin-sidenav");

        Div drawer = new Div(logo, sideNav);
        drawer.addClassName("admin-drawer");
        addToDrawer(drawer);
    }

    private Div spacer() {
        Div spacer = new Div();
        spacer.getStyle().set("flex", "1");
        return spacer;
    }
}
