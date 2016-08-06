package name.wind.tools.process.browser;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import name.wind.tools.process.browser.events.FXMLLocation;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped @FXMLLocation(FXMLResources.FXML__RUN) public class RunStageController
    extends StageController implements ResourceBundleAware {

    @FXML protected TextField commandTextField;
    @FXML protected Button okButton;

    @Override protected void start(Stage stage, String identifier, Map<String, Object> parameters) {
        super.start(stage, identifier, parameters);
        stage.setTitle(bundle.getString("stage.run.title"));

        okButton.disableProperty().bind(
            Bindings.isEmpty(commandTextField.textProperty()));
    }

}
