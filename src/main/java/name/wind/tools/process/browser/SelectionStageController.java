package name.wind.tools.process.browser;

import com.sun.jna.platform.win32.Win32Exception;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.events.Action;
import name.wind.tools.process.browser.events.ActionEngage;
import name.wind.tools.process.browser.events.FXMLLocation;
import name.wind.tools.process.browser.windows.WindowHandle;
import name.wind.tools.process.browser.windows.WindowRoutines;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

@ApplicationScoped @FXMLLocation(FXMLResources.FXML__SELECTION) public class SelectionStageController
    extends StageController implements WindowRoutines, ResourceBundleAware {

    @Inject @Action("makeFullscreen") protected Event<ActionEngage<WindowHandle>> makeFullscreenActionEngage;

    @FXML protected ListView<WindowHandle> selectionListView;
    @FXML protected Button selectButton;

    @Override protected Dimension2D preferredStageSize() {
        return Value.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 3, visualBounds.getHeight() / 3))
            .get();
    }

    @Override @SuppressWarnings("unchecked") protected void start(Stage stage, String identifier, Map<String, Object> parameters) {
        super.start(stage, identifier, parameters);

        stage.setTitle(
            bundle.getString("stage.selection.title"));
        selectionListView.getItems().addAll(
            (Collection<? extends WindowHandle>) parameters.get("windowHandles"));
        selectButton.disableProperty().bind(Bindings.isNull(
            selectionListView.getSelectionModel().selectedItemProperty()));
    }

    @FXML protected void mouseClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1) {
            makeFullscreenActionEngage.fire(
                new ActionEngage<>(selectionListView.getSelectionModel().getSelectedItem()));
        }
    }

    @FXML protected void selectButtonPressed(ActionEvent event) {
        makeFullscreenActionEngage.fire(
            new ActionEngage<>(selectionListView.getSelectionModel().getSelectedItem()));
    }

    protected void makeFullscreen(@Observes @Action("makeFullscreen") ActionEngage<WindowHandle> actionEngage) {
        if (stage != null) {
            stage.hide();
        }

        WindowHandle windowHandle = actionEngage.target();

        try {
            removeWindowFrame(windowHandle);
            applyMonitorSizeToWindow(windowHandle);
        } catch (Throwable thrown) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setHeaderText(thrown.getMessage());
            errorAlert.show();
        }
    }

}
