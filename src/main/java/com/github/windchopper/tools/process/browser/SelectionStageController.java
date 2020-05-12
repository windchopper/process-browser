package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.Action;
import com.github.windchopper.common.fx.cdi.ActionEngage;
import com.github.windchopper.common.fx.cdi.form.Form;
import com.github.windchopper.common.util.Pipeliner;
import com.sun.jna.platform.win32.Win32Exception;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

@ApplicationScoped @Form(FXMLResources.FXML__SELECTION) public class SelectionStageController extends AnyStageController {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

    @Inject @Action("makeFullscreen") protected Event<ActionEngage<WindowInfo<?>>> makeFullscreenActionEngage;

    @FXML protected ListView<WindowInfo<?>> selectionListView;
    @FXML protected Button selectButton;

    @Override protected Dimension2D preferredStageSize() {
        return Pipeliner.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 3, visualBounds.getHeight() / 3))
            .get();
    }

    @Override @SuppressWarnings("unchecked") protected void afterLoad(Parent form, Map<String, ?> parameters, Map<String, ?> formNamespace) {
        super.afterLoad(form, parameters, formNamespace);
        stage.setTitle(bundle.getString("stage.selection.title"));
        selectionListView.getItems().addAll((Collection<WindowInfo<?>>) parameters.get("windowHandles"));
        selectButton.disableProperty().bind(Bindings.isNull(selectionListView.getSelectionModel().selectedItemProperty()));
    }

    @FXML protected void mouseClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1) {
            makeFullscreenActionEngage.fire(new ActionEngage<>(selectionListView.getSelectionModel().getSelectedItem()));
        }
    }

    @FXML protected void selectButtonPressed(ActionEvent event) {
        makeFullscreenActionEngage.fire(new ActionEngage<>(selectionListView.getSelectionModel().getSelectedItem()));
    }

    protected void makeFullscreen(@Observes @Action("makeFullscreen") ActionEngage<WindowInfo<?>> actionEngage) {
        stage.hide();

        try {
            actionEngage.target().makeFullScreen();
        } catch (Win32Exception thrown) {
            Alert errorAlert = prepareAlert(() -> new Alert(Alert.AlertType.ERROR));
            errorAlert.setHeaderText(thrown.getMessage());
            errorAlert.show();
        }
    }

}
