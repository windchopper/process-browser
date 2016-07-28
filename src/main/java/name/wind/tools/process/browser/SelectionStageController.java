package name.wind.tools.process.browser;

import javafx.event.ActionEvent;
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
import javafx.stage.Stage;
import javafx.util.Callback;
import name.wind.common.util.Builder;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.events.SelectionPerformed;
import name.wind.tools.process.browser.events.SelectionStageConstructed;
import name.wind.tools.process.browser.events.StageConstructed;
import name.wind.tools.process.browser.windows.WindowHandle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

@ApplicationScoped public class SelectionStageController extends AbstractStageController {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("name.wind.tools.process.browser.i18n.messages");

    @Inject private Event<SelectionPerformed<WindowHandle>> selectionPerformedEvent;

    private ListView<WindowHandle> selectionListView;

    private Parent buildSceneRoot(List<WindowHandle> windowHandles) {
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
                .add(target -> target::getItems, windowHandles)
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
        selectionPerformedEvent.fire(
            new SelectionPerformed<>(
                selectionListView.getSelectionModel().getSelectedItem()));
    }

    protected void start(@Observes @Named(StageConstructed.IDENTIFIER__SELECTION) SelectionStageConstructed<WindowHandle> stageConstructed) {
        super.start(
            stageConstructed.stage(),
            stageConstructed.identifier(),
            stageConstructed.preferredSize());

        Stage selectionStage = Builder.direct(() -> stage)
            .set(target -> target::setTitle, bundle.getString("name.wind.tools.process.browser.SelectionStageController.title"))
            .set(target -> target::setScene, Builder.direct(() -> new Scene(buildSceneRoot(stageConstructed.objects())))
                .add(target -> target::getStylesheets, singletonList("/name/wind/tools/process/browser/selectionStage.css"))
                .get())
            .get();

        selectionStage.show();
    }


}
