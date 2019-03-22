module windchopper.tools.process.browser {

    opens com.github.windchopper.tools.process.browser;
    opens com.github.windchopper.tools.process.browser.jna;
    opens com.github.windchopper.tools.process.browser.images;

    requires java.prefs;
    requires java.logging;

    requires javax.inject;

    requires javafx.controls;
    requires javafx.fxml;

    requires cdi.api;

    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires weld.se.core;
    requires weld.environment.common;
    requires weld.core.impl;

    requires windchopper.common;

}