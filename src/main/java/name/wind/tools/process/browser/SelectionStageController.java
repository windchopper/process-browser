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
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

@ApplicationScoped @FXMLLocation("/name/wind/tools/process/browser/selectionStage.fxml") public class SelectionStageController
    extends StageController implements ResourceBundleAware {

//    @Inject private Event<SelectionPerformed<WindowHandle>> selectionPerformedEvent;

    private ListView<WindowHandle> selectionListView;

    private Parent buildSceneRoot() {
        return Builder.direct(BorderPane::new)
            .set(target -> target::setPadding, new Insets(4, 4, 4, 4))
            .set(target -> target::setTop, Builder.direct(Label::new)
                .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.SelectionStageController.label"))
                .set(target -> target::setPadding, new Insets(10, 8, 10, 8))
                .get())
            .set(target -> target::setCenter, selectionListView = Builder.direct((Supplier<ListView<WindowHandle>>) ListView::new)
                .set(target -> target::setCellFactory, new Callback<ListView<WindowHandle>, ListCell<WindowHandle>>() {
                    @Override public ListCell<WindowHandle> call(ListView<WindowHandle> listView) {
                        return new ListCell<WindowHandle>() {
                            @Override protected void updateItem(WindowHandle item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(empty ? "" : Value.of(item)
                                    .map(WindowHandle::title)
                                    .orElse(bundle.getString("name.wind.tools.process.browser.SelectionStageController.emptyTitle")));
                            }
                        };
                    }
                })
                //.add(target -> target::getItems, windowHandles)
                .get())
            .set(target -> target::setBottom, Builder.direct(HBox::new)
                .set(target -> target::setAlignment, Pos.BASELINE_RIGHT)
                .set(target -> target::setPadding, new Insets(4, 0, 0, 0))
                .add(target -> target::getChildren, singletonList(
                    Builder.direct(Button::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.SelectionStageController.select"))
                        .set(target -> target::setDefaultButton, true)
                        .set(target -> target::setOnAction, this::select)
                        .get()))
                .get())
            .get();
    }

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

    @Override protected void start(Stage stage, String identifier) {
        super.start(stage, identifier);

        Stage selectionStage = Builder.direct(() -> stage)
            .set(target -> target::setTitle, bundle.getString("name.wind.tools.process.browser.SelectionStageController.title"))
            .set(target -> target::setScene, new Scene(buildSceneRoot()))
            .get();

        selectionStage.show();
    }


}
