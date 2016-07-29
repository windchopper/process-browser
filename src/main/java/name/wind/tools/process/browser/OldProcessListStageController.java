package name.wind.tools.process.browser;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import name.wind.common.fx.Action;
import name.wind.common.fx.Alignment;
import name.wind.common.fx.Fill;
import name.wind.common.util.Builder;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.events.SelectionPerformed;
import name.wind.tools.process.browser.events.SelectionStageConstructed;
import name.wind.tools.process.browser.events.StageConstructed;
import name.wind.tools.process.browser.windows.ExecutableHandle;
import name.wind.tools.process.browser.windows.ProcessHandle;
import name.wind.tools.process.browser.windows.ProcessModuleHandle;
import name.wind.tools.process.browser.windows.WindowHandle;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@ApplicationScoped public class OldProcessListStageController extends OldAbstractStageController implements WindowRoutines {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("name.wind.tools.process.browser.i18n.messages");

    @Inject @Named("single") private Executor executor;
    @Inject @Named(StageConstructed.IDENTIFIER__SELECTION) private Event<SelectionStageConstructed<WindowHandle>> selectionEvent;
    @Inject private Event<SelectionPerformed<WindowHandle>> selectionPerformedEvent;

    private Action refreshAction;
    private Action makeFullscreenAction;
    private Action terminateAction;
    private TreeTableView<ExecutableHandle> processTreeTableView;

    @PostConstruct protected void initialize() {
        refreshAction = Builder.direct(Action::new)
            .set(target -> target.textProperty()::set, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.toolBar.button.refresh"))
            .set(target -> target::setHandler, this::refresh)
            .set(target -> target::setExecutor, executor)
            .get();
        makeFullscreenAction = Builder.direct(Action::new)
            .set(target -> target.textProperty()::set, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.toolBar.button.makeFullscreen"))
            .set(target -> target::setHandler, this::makeFullscreen)
            .set(target -> target::setExecutor, executor)
            .get();
        terminateAction = Builder.direct(Action::new)
            .set(target -> target.textProperty()::set, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.toolBar.button.terminate"))
            .set(target -> target::setHandler, this::terminate)
            .set(target -> target::setExecutor, executor)
            .get();
    }

    private Parent buildSceneRoot() {
        TreeItem<ExecutableHandle> root = new TreeItem<>(null);
        root.setExpanded(true);

        loadProcessTree(root);

        Callback<TreeTableColumn<ExecutableHandle, String>, TreeTableCell<ExecutableHandle, String>> cellFactory = column -> new TreeTableCell<ExecutableHandle, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                TreeTableRow<ExecutableHandle> row = getTreeTableRow();
                setStyle(row.getTreeItem() != null && row.getTreeItem().getValue() instanceof ProcessModuleHandle
                    ? "-fx-text-fill: #3b3b3b; -fx-font-style: italic"
                    : "-fx-text-fill: #000000; -fx-font-style: normal");
            }
        };

        return Builder.direct(BorderPane::new)
            .set(target -> target::setPadding, new Insets(4, 4, 4, 4))
            .set(target -> target::setTop, Builder.direct(GridPane::new)
                .add(target -> target::getColumnConstraints, asList(
                    Builder.direct(ColumnConstraints::new)
                        .set(constraints -> constraints::setHgrow, Priority.ALWAYS)
                        .set(constraints -> constraints::setPercentWidth, 100.)
                        .get()))
                .add(target -> target::getChildren, asList(
                    Builder.direct(TextField::new)
                        .set(target -> target::setPromptText, "Filter")
                        .accept(target -> GridPane.setConstraints(target, 0, 0, 1, 1))
                        .accept(target -> GridPane.setMargin(target, new Insets(0, 0, 4, 0)))
                        .accept(Alignment.LEFT_BASELINE::apply)
                        .accept(Fill.HORIZONTAL::apply)
                        .get()
                ))
                .get())
            .set(target -> target::setCenter, processTreeTableView = Builder.direct(TreeTableView<ExecutableHandle>::new)
                .set(target -> target::setShowRoot, false)
                .set(target -> target::setColumnResizePolicy, TreeTableView.CONSTRAINED_RESIZE_POLICY)
                .set(target -> target.getSelectionModel()::setSelectionMode, SelectionMode.SINGLE)
                .add(target -> target::getColumns, asList(
                    Builder.direct((Supplier<TreeTableColumn<ExecutableHandle, String>>) TreeTableColumn::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.table.column.identifier"))
                        .set(target -> target::setMaxWidth, Integer.MAX_VALUE * 10.)
                        .set(target -> target::setCellValueFactory, features -> new ReadOnlyStringWrapper(
                            Value.of(features.getValue().getValue())
                                .filter(ProcessHandle.class::isInstance)
                                .map(ProcessHandle.class::cast)
                                .map(ProcessHandle::identifier)
                                .map(Object::toString)
                                .orElse(null)))
                        .get(),
                    Builder.direct((Supplier<TreeTableColumn<ExecutableHandle, String>>) TreeTableColumn::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.table.column.name"))
                        .set(target -> target::setMaxWidth, Integer.MAX_VALUE * 20.)
                        .set(target -> target::setCellFactory, cellFactory)
                        .set(target -> target::setCellValueFactory, features -> new ReadOnlyStringWrapper(
                            Value.of(features.getValue().getValue())
                                .map(ExecutableHandle::name)
                                .orElse(null)))
                        .get(),
                    Builder.direct((Supplier<TreeTableColumn<ExecutableHandle, String>>) TreeTableColumn::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.table.column.executablePath"))
                        .set(target -> target::setMaxWidth, Integer.MAX_VALUE * 70.)
                        .set(target -> target::setCellFactory, cellFactory)
                        .set(target -> target::setCellValueFactory, features -> new ReadOnlyStringWrapper(
                            Value.of(features.getValue().getValue())
                                .map(ExecutableHandle::path)
                                .map(Object::toString)
                                .orElse(null)))
                        .get()))
                .set(target -> target::setRoot, root)
                .set(target -> target::setContextMenu, Builder.direct(ContextMenu::new)
                    .add(target -> target::getItems, asList(
                        Builder.direct(MenuItem::new)
                            .accept(refreshAction::bind)
                            .get(),
                        Builder.direct(SeparatorMenuItem::new)
                            .get(),
                        Builder.direct(MenuItem::new)
                            .accept(makeFullscreenAction::bind)
                            .get(),
                        Builder.direct(MenuItem::new)
                            .accept(terminateAction::bind)
                            .get()
                    ))
                    .get())
                .get())
            .get();
    }

    private void loadProcessTree(TreeItem<ExecutableHandle> root) {
        List<ProcessHandle> processHandles = ProcessHandle.allAvailable();
        Platform.runLater(() -> {
            root.getChildren().clear();
            processHandles
                .forEach(processInformation -> {
                    TreeItem<ExecutableHandle> processItem = new TreeItem<>(processInformation);
                    root.getChildren().add(processItem);

                    processInformation.modules()
                        .forEach(processModuleInformation -> processItem.getChildren().add(
                            new TreeItem<>(processModuleInformation)));
                });
        });
    }

    private void refresh(ActionEvent event) {
        loadProcessTree(processTreeTableView.getRoot());
    }

    private void makeFullscreen(ActionEvent event) {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();

        List<WindowHandle> windowHandles = processWindowHandles(
            (ProcessHandle) selectedItem.getValue());

        if (windowHandles.size() > 1) {
            Platform.runLater(() -> selectionEvent.fire(
                new SelectionStageConstructed<>(
                    Builder.direct(Stage::new)
                        .set(target -> target::initOwner, stage)
                        .set(target -> target::initModality, Modality.APPLICATION_MODAL)
                        .set(target -> target::setResizable, false)
                        .get(),
                    StageConstructed.IDENTIFIER__SELECTION,
                    Value.of(Screen.getPrimary().getVisualBounds())
                        .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 3, visualBounds.getHeight() / 3))
                        .get(),
                    windowHandles)));
        } else if (windowHandles.size() > 0) {
            selectionPerformedEvent.fire(
                new SelectionPerformed<>(windowHandles.get(0)));
        }
    }

    protected void windowHandleSelected(@Observes SelectionPerformed<WindowHandle> selectionPerformed) {
        System.out.println("hwnd: " + selectionPerformed.selectedObject().handle());
        System.out.println("title: " + selectionPerformed.selectedObject().title());
    }

    private void terminate(ActionEvent event) {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();

        FutureTask<Boolean> confirmationTask = new FutureTask<>(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO);
            alert.setHeaderText(bundle.getString("name.wind.tools.process.browser.ProcessListStageController.confirm.terminate"));
            return alert.showAndWait()
                .map(choice -> choice == ButtonType.YES)
                .orElse(false);
        });

        Platform.runLater(confirmationTask);
        Value<String> errorMessage = Value.empty();

        try {
            if (confirmationTask.get()) {
                ProcessHandle processHandle = (ProcessHandle) selectedItem.getValue();
                processHandle.terminate(0);

                selectedItem.getParent().getChildren().remove(selectedItem);
            }
        } catch (InterruptedException thrown) {
            errorMessage = Value.of(bundle.getString("name.wind.tools.process.browser.ProcessListStageController.error.interrupted"));
        } catch (ExecutionException thrown) {
            errorMessage = Value.of(String.format(bundle.getString("name.wind.tools.process.browser.ProcessListStageController.error.unexpected"), thrown.getMessage()));
        }

        errorMessage.ifPresent(message -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(message);
            alert.show();
        }));
    }

    private boolean selectionIsProcessHandle() {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();
        return selectedItem != null && selectedItem.getValue() instanceof ProcessHandle;
    }

    protected void start(@Observes @Named(StageConstructed.IDENTIFIER__PROCESS_LIST) StageConstructed stageConstructed) {
        super.start(
            stageConstructed.stage(),
            stageConstructed.identifier(),
            stageConstructed.preferredSize());

        Stage processListStage = Builder.direct(() -> stage)
            .set(target -> target::setTitle, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.title"))
            .set(target -> target::setScene, Builder.direct(() -> new Scene(buildSceneRoot()))
                .add(target -> target::getStylesheets, singletonList("/name/wind/tools/process/browser/processListStage.css"))
                .get())
            .get();

        BooleanBinding selectionIsProcessHandle = Bindings.createBooleanBinding(
            this::selectionIsProcessHandle, processTreeTableView.getSelectionModel().selectedItemProperty());
        Stream.of(makeFullscreenAction, terminateAction).forEach(
            action -> action.disabledProperty().bind(selectionIsProcessHandle.not()));

        processTreeTableView.requestFocus();
        processListStage.show();
    }

}
