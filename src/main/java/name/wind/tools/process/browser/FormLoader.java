package name.wind.tools.process.browser;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import name.wind.tools.process.browser.events.FormOpen;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped public class FormLoader {

    @Inject private FXMLLoader fxmlLoader;

    protected void formOpen(@Observes FormOpen formOpen) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(formOpen.resource())) {
            StageController controller = (StageController) formOpen.controllerReference().resolve();
            fxmlLoader.setController(controller);
            Parent sceneRoot = fxmlLoader.load(inputStream);
            Scene scene = new Scene(sceneRoot);
            Stage stage = formOpen.stage();
            stage.setScene(scene);
            controller.start(stage, formOpen.resource());
            stage.show();
        }
    }

}
