package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.ResourceBundleLoad;
import com.github.windchopper.common.fx.cdi.form.StageFormLoad;
import com.github.windchopper.common.util.ClassPathResource;
import javafx.application.Application;
import javafx.stage.Stage;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import java.util.ResourceBundle;

public class Launcher extends Application {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

    private Weld weld;
    private WeldContainer weldContainer;

    @Override public void init() {
        weld = new Weld();
        weldContainer = weld.initialize();
    }

    @Override public void stop() {
        weld.shutdown();
    }

    @Override public void start(Stage primaryStage) {
        var beanManager = weldContainer.getBeanManager();

        beanManager.fireEvent(
            new ResourceBundleLoad(bundle));
        beanManager.fireEvent(
            new StageFormLoad(new ClassPathResource(FXMLResources.FXML__PROCESS_LIST), () -> primaryStage));
    }

    public static void main(String... args) {
        launch(args);
    }

}
