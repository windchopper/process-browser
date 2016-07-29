package name.wind.tools.process.browser;

import javafx.application.Application;
import javafx.stage.Stage;
import name.wind.tools.process.browser.events.FormOpen;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class Launcher extends Application {

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
//        weldContainer.getBeanManager().fireEvent(
//            new StageConstructed(
//                primaryStage, StageConstructed.IDENTIFIER__PROCESS_LIST, Value.of(Screen.getPrimary().getVisualBounds())
//                    .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 2, visualBounds.getHeight() / 2))
//                    .get()),
//            new NamedLiteral(
//                StageConstructed.IDENTIFIER__PROCESS_LIST));
        weldContainer.getBeanManager().fireEvent(
            new FormOpen(primaryStage, "/name/wind/tools/process/browser/processListStage.fxml"));
    }

    /*
     *
     */

    public static void main(String... args) {
        launch(args);
    }

}
