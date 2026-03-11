package com.finly.backend.admin.config;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;

@Theme("finly-admin")
@Viewport("width=device-width, initial-scale=1")
@CssImport("./styles/admin-styles.css")
@Push(PushMode.AUTOMATIC)
public class AdminAppShell implements AppShellConfigurator {
}
