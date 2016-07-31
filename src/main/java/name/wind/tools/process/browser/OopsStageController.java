package name.wind.tools.process.browser;

import javafx.geometry.Dimension2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.events.FXMLLocation;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped @FXMLLocation(FXMLResources.FXML__OOPS) public class OopsStageController
    extends StageController implements ResourceBundleAware {

    @Override protected Dimension2D preferredStageSize() {
        return Value.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 3, visualBounds.getHeight() / 3))
            .get();
    }

    @Override protected void start(Stage stage, String identifier, Map<String, Object> parameters) {
        super.start(stage, identifier, parameters);
        stage.setTitle(bundle.getString("stage.oops.title"));
    }

}
