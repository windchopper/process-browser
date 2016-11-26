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
import name.wind.common.util.KnownSystemProperties;
import name.wind.common.util.Pipeliner;
import name.wind.tools.process.browser.windows.ProcessRoutines;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped @FXMLResource(FXMLResources.FXML__RUN) public class RunStageController
    extends AnyStageController implements ResourceBundleAware, PreferencesAware {

    @FXML protected TextField commandTextField;
    @FXML protected CheckBox elevateCheckBox;
    @FXML protected Button okButton;

    @FXML protected void browse(ActionEvent event) {
        Optional.ofNullable(
            Pipeliner.of(FileChooser::new)
                .set(chooser -> chooser::setInitialDirectory, Optional.ofNullable(browseInitialDirectoryPreferencesEntry.get())
                    .orElse(KnownSystemProperties.userHomeFile.get().orElse(null)))
                .map(chooser -> chooser.showOpenDialog(stage))
                .get())
            .ifPresent(selectedFile -> {
                browseInitialDirectoryPreferencesEntry.accept(
                    selectedFile.getParentFile());
                commandTextField.setText(
                    selectedFile.getAbsolutePath());
            });
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
            Pipeliner.of(() -> stage)
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
