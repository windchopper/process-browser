package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.application.fx.annotation.FXMLResource;
import com.github.windchopper.common.util.KnownSystemProperties;
import com.github.windchopper.common.util.Pipeliner;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@ApplicationScoped @FXMLResource(FXMLResources.FXML__RUN) public class RunStageController
    extends AnyStageController implements PreferencesAware {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

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
            new ProcessBuilder(commandTextField.getText());
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
            Bindings.createBooleanBinding(() -> false).not());
    }

}
