package name.wind.tools.process.browser.events;

import javafx.geometry.Dimension2D;
import javafx.stage.Stage;

public class StageConstructed {

    public static final String IDENTIFIER__PROCESS_LIST = "processListStage";
    public static final String IDENTIFIER__SELECTION = "selectionStage";

    private final Stage stage;
    private final String identifier;
    private final Dimension2D preferredSize;

    public StageConstructed(Stage stage, String identifier, Dimension2D preferredSize) {
        this.stage = stage;
        this.identifier = identifier;
        this.preferredSize = preferredSize;
    }

    public Stage stage() {
        return stage;
    }

    public String identifier() {
        return identifier;
    }

    public Dimension2D preferredSize() {
        return preferredSize;
    }

}
