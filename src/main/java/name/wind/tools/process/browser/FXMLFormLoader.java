package name.wind.tools.process.browser;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import name.wind.common.cdi.BeanReference;
import name.wind.tools.process.browser.events.FXMLLocationLiteral;
import name.wind.tools.process.browser.events.FXMLFormOpen;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped public class FXMLFormLoader {

    @Inject private FXMLLoader fxmlLoader;

    protected void formOpen(@Observes FXMLFormOpen fxmlFormOpen) throws IOException {
        try (InputStream inputStream = fxmlFormOpen.resourceAsStream()) {
            FXMLLocationLiteral fxmlLocationLiteral = new FXMLLocationLiteral(fxmlFormOpen.resource());
            BeanReference controllerReference = new BeanReference().withType(StageController.class).withQualifiers(fxmlLocationLiteral);
            StageController controller = (StageController) controllerReference.resolve();
            fxmlLoader.setController(controller);
            Parent sceneRoot = fxmlLoader.load(inputStream);
            Scene scene = new Scene(sceneRoot);
            Stage stage = fxmlFormOpen.stage();
            stage.setScene(scene);
            controller.start(stage, fxmlFormOpen.resource(), fxmlFormOpen.parameters());
            stage.show();
        }
    }

}
