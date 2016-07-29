package name.wind.tools.process.browser.events;

import javafx.stage.Stage;

public class FormShow {

    private final Stage stage;
    private final String identifier;

    public FormShow(Stage stage, String identifier) {
        this.stage = stage;
        this.identifier = identifier;
    }

    public Stage stage() {
        return stage;
    }

    public String identifier() {
        return identifier;
    }

}
