package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.form.StageFormController;
import com.github.windchopper.common.util.Pipeliner;
import com.github.windchopper.common.util.ReinforcedSupplier;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;

public abstract class AnyStageController extends StageFormController {

    @Override protected void afterLoad(Parent form, Map<String, ?> parameters, Map<String, ?> formNamespace) {
        super.afterLoad(form, parameters, formNamespace);
        stage.getIcons().add(new Image("/com/github/windchopper/tools/process/browser/images/Show-All-Views-50.png"));
    }

    ReinforcedSupplier<Alert> prepareAlert(Alert.AlertType type, ButtonType... buttonTypes) {
        return Pipeliner.of(() -> new Alert(type, null, buttonTypes))
            .set(bean -> bean::initOwner, stage)
            .set(bean -> bean::initModality, Modality.APPLICATION_MODAL)
            .accept(bean -> ((Stage) bean.getDialogPane().getScene().getWindow()).getIcons().addAll(
                stage.getIcons()));
    }

}
