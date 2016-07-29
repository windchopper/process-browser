package name.wind.tools.process.browser.events;

import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class FXMLFormOpen {

    private final Stage stage;
    private final String resource;
    private final Map<String, Object> parameters;

    public FXMLFormOpen(Stage stage, String resource) {
        this(stage, resource, emptyMap());
    }

    public FXMLFormOpen(Stage stage, String resource, Map<String, Object> parameters) {
        this.stage = stage;
        this.resource = resource;
        this.parameters = parameters;
    }

    public Stage stage() {
        return stage;
    }

    public String resource() {
        return resource;
    }

    public InputStream resourceAsStream() {
        return getClass().getResourceAsStream(resource);
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

}
