package name.wind.tools.process.browser.events;

import javafx.geometry.Dimension2D;
import javafx.stage.Stage;

import java.util.List;

public class SelectionStageConstructed<T> extends StageConstructed {

    private final List<T> objects;

    public SelectionStageConstructed(Stage stage, String identifier, Dimension2D preferredSize, List<T> objects) {
        super(stage, identifier, preferredSize);
        this.objects = objects;
    }

    public List<T> objects() {
        return objects;
    }

}
