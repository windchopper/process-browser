package name.wind.tools.process.browser;

import javafx.event.ActionEvent;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import name.wind.common.util.Builder;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.events.FXMLLocation;
import name.wind.tools.process.browser.windows.WindowHandle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

@ApplicationScoped @FXMLLocation("/name/wind/tools/process/browser/selectionStage.fxml") public class SelectionStageController
    extends StageController implements ResourceBundleAware {

//    @Inject private Event<SelectionPerformed<WindowHandle>> selectionPerformedEvent;

    private ListView<WindowHandle> selectionListView;

    private void select(ActionEvent event) {
        stage.hide();
//        selectionPerformedEvent.fire(
//            new SelectionPerformed<>(
//                selectionListView.getSelectionModel().getSelectedItem()));
    }

    @Override protected Dimension2D preferredStageSize() {
        return Value.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 3, visualBounds.getHeight() / 3))
            .get();
    }

}
