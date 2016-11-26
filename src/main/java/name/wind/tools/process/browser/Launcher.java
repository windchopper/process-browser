package name.wind.tools.process.browser;

import javafx.application.Application;
import javafx.stage.Stage;
import name.wind.application.cdi.event.ResourceBundleLoading;
import name.wind.application.cdi.fx.event.FXMLResourceOpen;
import name.wind.common.util.KnownSystemProperties;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import javax.enterprise.inject.spi.BeanManager;

public class Launcher extends Application implements KnownSystemProperties, ResourceBundleAware {

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

    @Override public void start(Stage primaryStage) throws Exception {
        BeanManager beanManager = weldContainer.getBeanManager();
        beanManager.fireEvent(
            new ResourceBundleLoading(bundle));
        beanManager.fireEvent(
            new FXMLResourceOpen(primaryStage, operationSystemName.get().map(String::toLowerCase).orElse("unknown").contains("windows")
                ? FXMLResources.FXML__PROCESS_LIST
                : FXMLResources.FXML__NOT_WINDOWS));
    }

    /*
     *
     */

    public static void main(String... args) {
        launch(args);
    }

}
