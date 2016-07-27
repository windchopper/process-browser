package name.wind.tools.process.browser;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import name.wind.common.util.Builder;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.events.StageConstructed;
import name.wind.tools.process.browser.windows.ExecutableHandle;
import name.wind.tools.process.browser.windows.ProcessHandle;
import name.wind.tools.process.browser.windows.ProcessModuleHandle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static name.wind.tools.process.browser.windows.ProcessHandle.allAvailable;
import static name.wind.tools.process.browser.windows.ProcessHandle.launchElevated;

@ApplicationScoped public class ProcessListStageController extends AbstractStageController implements WindowsRoutines {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("name.wind.tools.process.browser.i18n.messages");

    private TreeTableView<ExecutableHandle> processTreeTableView;
    private Button relaunchAsAdministratorButton;
    private Button makeFullscreenButton;
    private Button terminateButton;

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
            .set(target -> target::setTop, Builder.direct(ToolBar::new)
                .add(target -> target::getItems, asList(
                    relaunchAsAdministratorButton = Builder.direct(Button::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.toolBar.button.runElevated"))
                        .set(target -> target::setOnAction, this::relaunchAsAdministrator)
                        .get(),
                    Builder.direct(Button::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.toolBar.button.refresh"))
                        .set(target -> target::setOnAction, this::refresh)
                        .get(),
                    Builder.direct(Separator::new)
                        .get(),
                    makeFullscreenButton = Builder.direct(Button::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.toolBar.button.makeFullscreen"))
                        .set(target -> target::setOnAction, this::makeFullscreen)
                        .get(),
                    terminateButton = Builder.direct(Button::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.toolBar.button.terminate"))
                        .set(target -> target::setOnAction, this::destroy)
                        .get()
                ))
                .get())
            .set(target -> target::setCenter, processTreeTableView = Builder.direct(TreeTableView<ExecutableHandle>::new)
                .set(target -> target.getSelectionModel()::setSelectionMode, SelectionMode.SINGLE)
                .set(target -> target::setShowRoot, false)
                .set(target -> target::setColumnResizePolicy, TreeTableView.CONSTRAINED_RESIZE_POLICY)
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
                .get())
            .get();
    }

    private void relaunchAsAdministrator(ActionEvent event) {
        try {
            launchElevated();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void loadProcessTree(TreeItem<ExecutableHandle> root) {
        root.getChildren().clear();

        allAvailable()
            .forEach(processInformation -> {
                TreeItem<ExecutableHandle> processItem = new TreeItem<>(processInformation);
                root.getChildren().add(processItem);

                processInformation.modules()
                    .forEach(processModuleInformation -> processItem.getChildren().add(
                        new TreeItem<>(processModuleInformation)));
            });
    }

    private void refresh(ActionEvent event) {
        loadProcessTree(processTreeTableView.getRoot());
    }

    private void makeFullscreen(ActionEvent event) {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();

        processWindows(
            (ProcessHandle) selectedItem.getValue());
    }

    private void destroy(ActionEvent event) {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();

        ((ProcessHandle) selectedItem.getValue()).destroy(0);

        selectedItem.getParent().getChildren().remove(selectedItem);
    }

    private boolean selectionIsProcessHandle() {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();
        return selectedItem != null && selectedItem.getValue() instanceof ProcessHandle;
    }

    public void start(@Observes @Named(StageConstructed.IDENTIFIER__PROCESS_LIST) StageConstructed stageConstructed) {
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
        Stream.of(makeFullscreenButton, terminateButton).forEach(
            button -> button.disableProperty().bind(selectionIsProcessHandle.not()));

        relaunchAsAdministratorButton.setDisable(
            ProcessHandle.hasAdministrativeRights() && ProcessHandle.elevated());

        processListStage.show();
    }

}
