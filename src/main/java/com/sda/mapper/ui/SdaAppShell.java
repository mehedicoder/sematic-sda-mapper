package com.sda.mapper.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

/**
 * Vaadin application shell. Enables server push application-wide so live
 * views (pipeline observability console, OPC UA monitor) receive streamed
 * updates without manual refresh.
 */
@Push
public class SdaAppShell implements AppShellConfigurator {
}
