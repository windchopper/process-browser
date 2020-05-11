package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.form.Form;
import com.github.windchopper.common.util.Pipeliner;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@ApplicationScoped @Form(FXMLResources.FXML__RUN) public class RunStageController
    extends AnyStageController implements PreferencesAware {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

    @FXML protected TextField commandTextField;
    @FXML protected CheckBox elevateCheckBox;
    @FXML protected Button okButton;

    @FXML protected void browse(ActionEvent event) {
        Optional.ofNullable(Pipeliner.of(FileChooser::new)
            .set(chooser -> chooser::setInitialDirectory, Optional.ofNullable(browseInitialDirectoryPreferencesEntry.load())
                .orElseGet(() -> Optional.ofNullable(System.getProperty("user.home"))
                    .map(File::new)
                    .orElse(null)))
            .get()
            .showOpenDialog(stage))
            .ifPresent(selectedFile -> {
                browseInitialDirectoryPreferencesEntry.save(
                    selectedFile.getParentFile());
                commandTextField.setText(
                    selectedFile.getAbsolutePath());
            });
    }

    @FXML protected void run(ActionEvent event) {
        try {
            new ProcessBuilder(commandTextField.getText())
                .start();
        } catch (Exception thrown) {
            Alert errorAlert = prepareAlert(() -> new Alert(Alert.AlertType.ERROR));
            errorAlert.setHeaderText(thrown.getMessage());
            errorAlert.show();
        }
    }

    @Override protected void afterLoad(Parent form, Map<String, ?> parameters, Map<String, ?> formNamespace) {
        super.afterLoad(form, parameters, formNamespace);

        stage.setTitle(
            bundle.getString("stage.run.title"));
        okButton.disableProperty().bind(
            Bindings.isEmpty(commandTextField.textProperty()));
        elevateCheckBox.disableProperty().bind(
            Bindings.createBooleanBinding(() -> false).not());
    }

}
