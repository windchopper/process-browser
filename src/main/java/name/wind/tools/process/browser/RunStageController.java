package name.wind.tools.process.browser;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import name.wind.application.cdi.fx.annotation.FXMLResource;
import name.wind.common.util.Builder;
import name.wind.common.util.KnownSystemProperties;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.windows.ProcessRoutines;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Map;

@ApplicationScoped @FXMLResource(FXMLResources.FXML__RUN) public class RunStageController
    extends CommonStageController implements ResourceBundleAware, PreferencesAware {

    @FXML protected TextField commandTextField;
    @FXML protected CheckBox elevateCheckBox;
    @FXML protected Button okButton;

    @FXML protected void browse(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(
            Value.of(browseInitialDirectoryPreferencesEntry)
                .orElse(KnownSystemProperties.PROPERTY__USER_HOME.value()));

        File selectedFile = chooser.showOpenDialog(stage);

        if (selectedFile != null) {
            browseInitialDirectoryPreferencesEntry.accept(
                selectedFile.getParentFile());
            commandTextField.setText(
                selectedFile.getAbsolutePath());
        }
    }

    @FXML protected void run(ActionEvent event) {
        try {
            ProcessRoutines.runProcess(commandTextField.getText(), null, elevateCheckBox.isSelected());
        } catch (Exception thrown) {
            Alert errorAlert = prepareAlert(() -> new Alert(Alert.AlertType.ERROR));
            errorAlert.setHeaderText(thrown.getMessage());
            errorAlert.show();
        }
    }

    @Override protected void start(Stage stage, String fxmlResource, Map<String, ?> parameters) {
        super.start(
            Builder.direct(() -> stage)
                .set(target -> target::setTitle, bundle.getString("stage.run.title"))
                .get(),
            fxmlResource,
            parameters);

        okButton.disableProperty().bind(
            Bindings.isEmpty(commandTextField.textProperty()));
        elevateCheckBox.disableProperty().bind(
            Bindings.createBooleanBinding(ProcessRoutines::currentProcessHasAdministrativeRights).not());
    }

}
