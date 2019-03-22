package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.event.FXMLResourceOpen;
import com.github.windchopper.common.fx.event.ResourceBundleLoading;
import com.github.windchopper.common.util.KnownSystemProperties;
import javafx.application.Application;
import javafx.stage.Stage;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import javax.enterprise.inject.spi.BeanManager;
import java.util.ResourceBundle;

public class Launcher extends Application implements KnownSystemProperties {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

    private Weld weld;
    private WeldContainer weldContainer;

    @Override public void init() throws Exception {
        super.init();
        weld = new Weld();
        weldContainer = weld.initialize();
    }

    @Override public void stop() throws Exception {
        weld.shutdown();
        super.stop();
    }

    @Override public void start(Stage primaryStage) {
        BeanManager beanManager = weldContainer.getBeanManager();
        beanManager.fireEvent(
            new ResourceBundleLoading(bundle));
        beanManager.fireEvent(
            new FXMLResourceOpen(primaryStage, FXMLResources.FXML__PROCESS_LIST));
    }

    /*
     *
     */

    public static void main(String... args) {
        launch(args);
    }

}
