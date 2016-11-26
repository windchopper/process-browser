package name.wind.tools.process.browser;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import name.wind.application.cdi.fx.StageController;
import name.wind.common.util.Pipeliner;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public abstract class AnyStageController extends StageController {

    protected final Image iconImage = new Image("/name/wind/tools/process/browser/images/Show-All-Views-50.png");

    @Override protected void start(Stage stage, String fxmlResource, Map<String, ?> parameters) {
        super.start(
            Pipeliner.of(() -> stage)
                .add(target -> target::getIcons, singletonList(iconImage))
                .get(),
            fxmlResource,
            parameters);
    }

    @Override protected Alert prepareAlert(Supplier<Alert> constructor) {
        return Pipeliner.of(() -> super.prepareAlert(constructor))
            .accept(alert -> ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(iconImage))
            .get();
    }

}
