package name.wind.tools.process.browser;

import com.sun.jna.platform.win32.Win32Exception;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import name.wind.common.util.Builder;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.events.FXMLLocation;
import name.wind.tools.process.browser.events.SelectionPerformed;
import name.wind.tools.process.browser.events.SelectionStageConstructed;
import name.wind.tools.process.browser.events.StageConstructed;
import name.wind.tools.process.browser.windows.ExecutableHandle;
import name.wind.tools.process.browser.windows.ProcessHandle;
import name.wind.tools.process.browser.windows.WindowHandle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

@ApplicationScoped @FXMLLocation("/name/wind/tools/process/browser/processListStage.fxml") public class ProcessListStageController
    extends StageController implements ResourceBundleAware, WindowRoutines {

    @Inject @Named(StageConstructed.IDENTIFIER__SELECTION) protected Event<SelectionStageConstructed<WindowHandle>> selectionEvent;
    @Inject protected Event<SelectionPerformed<WindowHandle>> selectionPerformedEvent;

    @FXML protected TreeTableView<ExecutableHandle> processTreeTableView;
    @FXML protected TextField filterTextField;
    @FXML protected MenuItem makeFullscreenMenuItem;
    @FXML protected MenuItem terminateMenuItem;

    protected final ExecutableHandleSearch processSearch = new ExecutableHandleSearch();
    protected List<ProcessHandle> lastLoadedProcessHandles;

    @Override protected void start(Stage stage, String identifier) {
        super.start(stage, identifier);

        TreeItem<ExecutableHandle> processTreeRoot = new TreeItem<>(null);
        processTreeRoot.setExpanded(true);

        loadProcessTree(processTreeRoot, lastLoadedProcessHandles = ProcessHandle.allAvailable());

        processTreeTableView.setRoot(processTreeRoot);
        processTreeTableView.requestFocus();

        BooleanBinding selectionIsProcessHandle = Bindings.createBooleanBinding(
            this::selectionIsProcessHandle, processTreeTableView.getSelectionModel().selectedItemProperty());
        Stream.of(makeFullscreenMenuItem, terminateMenuItem).forEach(
            menuItem -> menuItem.disableProperty().bind(selectionIsProcessHandle.not()));

        filterTextField.textProperty().addListener(this::filterTextChanged);
    }

    @Override protected Dimension2D preferredStageSize() {
        return Value.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 2, visualBounds.getHeight() / 2))
            .get();
    }

    protected void filterTextChanged(ObservableValue<? extends String> property, String oldValue, String newValue) {
        ExecutableHandleSearch.Continuation continuation = new ExecutableHandleSearch.Continuation();

        processSearch.search(
            continuation,
            object -> object instanceof ExecutableHandle && ((ExecutableHandle) object).name().toLowerCase().contains(newValue.toLowerCase()),
            lastLoadedProcessHandles);

        loadProcessTree(processTreeTableView.getRoot(), continuation.searchResult());
    }

    protected void loadProcessTree(TreeItem<ExecutableHandle> root, Collection<ProcessHandle> processHandles) {
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

    @FXML protected void refresh(ActionEvent event) {
        loadProcessTree(processTreeTableView.getRoot(), lastLoadedProcessHandles = ProcessHandle.allAvailable());
    }

    @FXML protected void makeFullscreen(ActionEvent event) {
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

    @FXML protected void terminate(ActionEvent event) {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO);
        confirmationAlert.initOwner(stage);
        confirmationAlert.setHeaderText(bundle.getString("name.wind.tools.process.browser.ProcessListStageController.confirm.terminate"));
        boolean terminate = confirmationAlert.showAndWait()
            .map(choice -> choice == ButtonType.YES)
            .orElse(false);

        Value<String> errorMessage = Value.empty();

        try {
            if (terminate) {
                ProcessHandle processHandle = (ProcessHandle) selectedItem.getValue();
                processHandle.terminate(0);

                selectedItem.getParent().getChildren().remove(selectedItem);
            }
        } catch (Win32Exception thrown) {
            errorMessage = Value.of(String.format(bundle.getString("name.wind.tools.process.browser.ProcessListStageController.error.unexpected"), thrown.getMessage()));
        }

        errorMessage.ifPresent(message -> Platform.runLater(() -> {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setHeaderText(message);
            errorAlert.show();
        }));
    }

    protected boolean selectionIsProcessHandle() {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();
        return selectedItem != null && selectedItem.getValue() instanceof ProcessHandle;
    }

}
