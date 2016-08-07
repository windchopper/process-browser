package name.wind.tools.process.browser;

import javafx.css.Styleable;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import name.wind.common.fx.behavior.WindowApplyStoredBoundsBehavior;
import name.wind.common.util.Builder;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.stream;

public abstract class StageController {

    protected final Image applicationImage = new Image("/name/wind/tools/process/browser/images/Show-All-Views-50.png");

    protected final WidgetSearch widgetSearch = new WidgetSearch();
    protected Stage stage;

    protected void start(Stage stage, String identifier, Map<String, Object> parameters) {
        new WindowApplyStoredBoundsBehavior(identifier, this::initializeBounds)
            .apply(this.stage = stage);

        stage.getIcons().add(applicationImage);

        stream(getClass().getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(FXML.class)).forEach(field -> {
                try {
                    WidgetSearch.Continuation continuation = new WidgetSearch.Continuation();
                    widgetSearch.search(continuation, object -> object instanceof Styleable && field.getName().equalsIgnoreCase(((Styleable) object).getId()), stage.getScene());
                    field.set(this, continuation.searchResult());
                } catch (IllegalAccessException ignored) {
                }
            });
    }

    protected Dimension2D preferredStageSize() {
        return null;
    }

    protected void initializeBounds(Window window, boolean resizable) {
        Dimension2D preferredSize = null;

        if (resizable) {
            preferredSize = preferredStageSize();
        }

        if (preferredSize == null) {
            window.sizeToScene();
        } else {
            window.setWidth(
                preferredSize.getWidth());
            window.setHeight(
                preferredSize.getHeight());
        }
    }

    protected Alert prepareAlert(Supplier<Alert> constructor) {
        return Builder.direct(constructor)
            .accept(alert -> ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(applicationImage))
            .set(alert -> alert::initOwner, stage)
            .get();
    }

}
