package name.wind.tools.process.browser;

import javafx.geometry.Dimension2D;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Window;
import name.wind.common.fx.behavior.WindowApplyStoredBoundsBehavior;
import name.wind.common.util.Value;

public abstract class OldAbstractStageController {

    protected Stage stage;

    protected void start(Stage stage, String identifier, Dimension2D preferredSize) {
        new WindowApplyStoredBoundsBehavior(identifier, window -> initializeInitialSize(window, preferredSize))
            .apply(this.stage = stage);
    }

    protected void initializeInitialSize(Window window, Dimension2D preferredSize) {
        if (preferredSize == null) {
            window.sizeToScene();
        } else {
            window.setWidth(preferredSize.getWidth());
            window.setHeight(preferredSize.getHeight());
        }
    }

    protected <T extends Node> T lookup(String selector, Class<T> type) {
        return Value.of(stage.getScene().lookup(selector))
            .filter(type::isInstance)
            .map(type::cast)
            .orElse(null);
    }

}
