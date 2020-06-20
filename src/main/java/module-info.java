module windchopper.tools.process.browser {

    opens com.github.windchopper.tools.process.browser;
    opens com.github.windchopper.tools.process.browser.images;
    opens com.github.windchopper.tools.process.browser.i18n;

    requires kotlin.stdlib;
    requires kotlin.stdlib.jdk8;

    requires kotlinx.coroutines.core;

    requires java.prefs;
    requires java.logging;

    requires javafx.controls;
    requires javafx.fxml;

    requires jakarta.inject.api;
    requires jakarta.enterprise.cdi.api;

    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires weld.se.core;
    requires weld.environment.common;

    requires windchopper.common.fx;
    requires windchopper.common.fx.cdi;
    requires windchopper.common.cdi;
    requires windchopper.common.preferences;
    requires windchopper.common.util;

}