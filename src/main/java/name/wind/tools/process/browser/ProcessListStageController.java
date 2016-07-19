package name.wind.tools.process.browser;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import name.wind.common.util.Builder;
import name.wind.tools.process.browser.events.StageConstructed;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@ApplicationScoped public class ProcessListStageController extends AbstractStageController {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("name.wind.tools.process.browser.i18n.messages");

    private TableView<ProcessHandle> processTableView;
    private Button refreshButton;
    private Button makeFullscreenButton;
    private Button terminateButton;

    private Parent buildSceneRoot() {
        return Builder.direct(BorderPane::new)
            .set(target -> target::setTop, Builder.direct(ToolBar::new)
                .add(target -> target::getItems, asList(
                    refreshButton = Builder.direct(Button::new)
                        .set(target -> target::setText, "Refresh")
                        .set(target -> target::setOnAction, this::refresh)
                        .get(),
                    Builder.direct(Separator::new)
                        .get(),
                    makeFullscreenButton = Builder.direct(Button::new)
                        .set(target -> target::setText, "Make fullscreen")
                        .set(target -> target::setOnAction, this::makeFullscreen)
                        .get(),
                    terminateButton = Builder.direct(Button::new)
                        .set(target -> target::setText, "Try terminate")
                        .set(target -> target::setOnAction, this::destroy)
                        .get()
                ))
                .get())
            .set(target -> target::setCenter, processTableView = Builder.direct(TableView<ProcessHandle>::new)
                .set(target -> target.getSelectionModel()::setSelectionMode, SelectionMode.SINGLE)
                .set(target -> target::setColumnResizePolicy, TableView.CONSTRAINED_RESIZE_POLICY)
                .add(target -> target::getColumns, asList(
                    Builder.direct((Supplier<TableColumn<ProcessHandle, Number>>) TableColumn::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.table.column.identifier"))
                        .set(target -> target::setMaxWidth, Integer.MAX_VALUE * 10.)
                        .set(target -> target::setCellValueFactory, features -> new ReadOnlyLongWrapper(
                            features.getValue().getPid()))
                        .get(),
                    Builder.direct((Supplier<TableColumn<ProcessHandle, String>>) TableColumn::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.table.column.name"))
                        .set(target -> target::setMaxWidth, Integer.MAX_VALUE * 20.)
                        .set(target -> target::setCellValueFactory, features -> new ReadOnlyStringWrapper(features.getValue().info().command()
                            .map(string -> Paths.get(string).getFileName().toString().replaceFirst("\\..+$", ""))
                            .orElse("")))
                        .get(),
                    Builder.direct((Supplier<TableColumn<ProcessHandle, String>>) TableColumn::new)
                        .set(target -> target::setText, bundle.getString("name.wind.tools.process.browser.ProcessListStageController.table.column.executablePath"))
                        .set(target -> target::setMaxWidth, Integer.MAX_VALUE * 70.)
                        .set(target -> target::setCellValueFactory, features -> new ReadOnlyStringWrapper(
                            features.getValue().info().command()
                                .orElse("")))
                        .get()))
                .add(target -> target::getItems, ProcessHandle.allProcesses()
                    .filter(processHandle -> processHandle.info().command().isPresent())
                    .collect(
                        toList()))
                .get())
            .get();
    }

    private void refresh(ActionEvent event) {
        processTableView.setItems(FXCollections.observableArrayList(ProcessHandle.allProcesses()
            .filter(processHandle -> processHandle.info().command().isPresent())
            .collect(
                toList())));
    }

    private void makeFullscreen(ActionEvent event) {
        ProcessHandle processHandle = processTableView.getSelectionModel().getSelectedItem();
        WindowsRoutines.listProcessWindows(processHandle);
    }

    private void destroy(ActionEvent event) {
        ProcessHandle processHandle = processTableView.getSelectionModel().getSelectedItem();
        if (processHandle.destroy()) {
            processTableView.getItems().remove(processHandle);
        }
    }

    private void start(@Observes @Named(StageConstructed.IDENTIFIER__PROCESS_LIST) StageConstructed stageConstructed) {
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

        BooleanBinding selectionEmptyBinding = Bindings.isNull(
            processTableView.getSelectionModel().selectedItemProperty());
        Stream.of(makeFullscreenButton, terminateButton).forEach(
            button -> button.disableProperty().bind(selectionEmptyBinding));

        processListStage.show();
    }

}
