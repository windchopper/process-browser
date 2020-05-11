package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.form.StageFormController;
import com.github.windchopper.common.util.Pipeliner;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;
import java.util.function.Supplier;

public abstract class AnyStageController extends StageFormController {

    protected final Image iconImage = new Image("/com/github/windchopper/tools/process/browser/images/Show-All-Views-50.png");

    @Override protected void afterLoad(Parent form, Map<String, ?> parameters, Map<String, ?> formNamespace) {
        super.afterLoad(form, parameters, formNamespace);
        stage.getIcons().add(iconImage);
    }

    protected Alert prepareAlert(Supplier<Alert> constructor) {
        return Pipeliner.of(constructor)
            .accept(alert -> alert.initOwner(stage))
            .accept(alert -> alert.initModality(Modality.APPLICATION_MODAL))
            .accept(alert -> ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(iconImage))
            .get();
    }

}
