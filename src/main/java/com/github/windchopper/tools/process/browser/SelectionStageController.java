package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.form.Form;
import com.github.windchopper.common.util.Pipeliner;
import com.github.windchopper.tools.process.browser.MakeFullScreenPerformer.MakeFullScreen;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

@ApplicationScoped @Form(Application.FXML__SELECTION) public class SelectionStageController extends AnyStageController {

    @Inject protected Event<MakeFullScreen> makeFullscreenEvent;

    @FXML protected ListView<WindowInfo<?>> selectionListView;
    @FXML protected Button selectButton;

    @Override protected Dimension2D preferredStageSize() {
        return Pipeliner.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 3, visualBounds.getHeight() / 3))
            .get();
    }

    @Override @SuppressWarnings("unchecked") protected void afterLoad(Parent form, Map<String, ?> parameters, Map<String, ?> formNamespace) {
        super.afterLoad(form, parameters, formNamespace);
        stage.setTitle(Application.messages.getString("stage.selection.title"));
        selectionListView.getItems().addAll((Collection<WindowInfo<?>>) parameters.get("windowHandles"));
        selectButton.disableProperty().bind(Bindings.isNull(selectionListView.getSelectionModel().selectedItemProperty()));
    }

    @FXML protected void mouseClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1) {
            makeFullscreenEvent.fire(new MakeFullScreen(this, selectionListView.getSelectionModel().getSelectedItem()));
        }
    }

    @FXML protected void selectButtonPressed(ActionEvent event) {
        makeFullscreenEvent.fire(new MakeFullScreen(this, selectionListView.getSelectionModel().getSelectedItem()));
    }

}
