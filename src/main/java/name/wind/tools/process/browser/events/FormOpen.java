package name.wind.tools.process.browser.events;

import javafx.stage.Stage;
import name.wind.common.cdi.BeanReference;

public class FormOpen {

    private final Stage stage;
    private final String resource;
    private final BeanReference controllerReference;

    public FormOpen(Stage stage, String resource) {
        this.stage = stage;
        controllerReference = new BeanReference().withQualifiers(
            new FXMLLocationLiteral(
                this.resource = resource));
    }

    public Stage stage() {
        return stage;
    }

    public String resource() {
        return resource;
    }

    public BeanReference controllerReference() {
        return controllerReference;
    }

}
