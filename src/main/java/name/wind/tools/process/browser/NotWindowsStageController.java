package name.wind.tools.process.browser;

import javafx.geometry.Dimension2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import name.wind.application.cdi.fx.annotation.FXMLResource;
import name.wind.common.util.Builder;
import name.wind.common.util.Value;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped @FXMLResource(FXMLResources.FXML__NOT_WINDOWS) public class NotWindowsStageController
    extends CommonStageController implements ResourceBundleAware {

    @Override protected Dimension2D preferredStageSize() {
        return Value.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 3, visualBounds.getHeight() / 3))
            .get();
    }

    @Override protected void start(Stage stage, String fxmlResource, Map<String, ?> parameters) {
        super.start(
            Builder.direct(() -> stage)
                .set(target -> target::setTitle, bundle.getString("stage.notWindows.title"))
                .get(),
            fxmlResource,
            parameters);
    }

}
