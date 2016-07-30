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
import name.wind.tools.process.browser.events.Action;
import name.wind.tools.process.browser.events.ActionEngage;
import name.wind.tools.process.browser.events.FXMLFormOpen;
import name.wind.tools.process.browser.events.FXMLLocation;
import name.wind.tools.process.browser.windows.ExecutableHandle;
import name.wind.tools.process.browser.windows.ProcessHandle;
import name.wind.tools.process.browser.windows.WindowHandle;
import name.wind.tools.process.browser.windows.WindowRoutines;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ApplicationScoped @FXMLLocation(FXMLResources.FXML__PROCESS_LIST) public class ProcessListStageController
    extends StageController implements ResourceBundleAware, WindowRoutines {

    @Inject protected Event<FXMLFormOpen> fxmlFormOpenEvent;
    @Inject @Action("makeFullscreen") protected Event<ActionEngage<WindowHandle>> makeFullscreenActionEngage;

    @FXML protected TreeTableView<ExecutableHandle> processTreeTableView;
    @FXML protected TextField filterTextField;
    @FXML protected MenuItem makeFullscreenMenuItem;
    @FXML protected MenuItem terminateMenuItem;

    private final ExecutableHandleSearch processSearch = new ExecutableHandleSearch();
    private List<ProcessHandle> lastLoadedProcessHandles;

    @Override protected void start(Stage stage, String identifier, Map<String, Object> parameters) {
        super.start(stage, identifier, parameters);

        stage.setTitle(bundle.getString("stage.processList.title"));

        TreeItem<ExecutableHandle> processTreeRoot = new TreeItem<>(null);
        processTreeRoot.setExpanded(true);

        loadProcessTree(processTreeRoot, lastLoadedProcessHandles = ProcessHandle.allAvailable());

        processTreeTableView.setRoot(processTreeRoot);
        processTreeTableView.requestFocus();

        BooleanBinding selectionIsProcessHandle = Bindings.createBooleanBinding(
            () -> Value.of(processTreeTableView.getSelectionModel().getSelectedItem()).filter(selectedItem -> selectedItem != null && selectedItem.getValue() instanceof ProcessHandle).present(),
            processTreeTableView.getSelectionModel().selectedItemProperty());
        Stream.of(makeFullscreenMenuItem, terminateMenuItem).forEach(
            menuItem -> menuItem.disableProperty().bind(selectionIsProcessHandle.not()));

        filterTextField.textProperty().addListener(this::filterTextChanged);
    }

    @Override protected Dimension2D preferredStageSize() {
        return Value.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 2, visualBounds.getHeight() / 2))
            .get();
    }

    private void filterTextChanged(ObservableValue<? extends String> property, String oldValue, String newValue) {
        ExecutableHandleSearch.Continuation continuation = new ExecutableHandleSearch.Continuation();

        processSearch.search(
            continuation,
            object -> object instanceof ExecutableHandle && ((ExecutableHandle) object).name().toLowerCase().contains(newValue.toLowerCase()),
            lastLoadedProcessHandles);

        loadProcessTree(processTreeTableView.getRoot(), continuation.searchResult());
    }

    private void loadProcessTree(TreeItem<ExecutableHandle> root, Collection<ProcessHandle> processHandles) {
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
            fxmlFormOpenEvent.fire(
                new FXMLFormOpen(
                    Builder.direct(Stage::new)
                        .set(target -> target::initOwner, stage)
                        .set(target -> target::initModality, Modality.APPLICATION_MODAL)
                        .set(target -> target::setResizable, false)
                        .get(),
                    FXMLResources.FXML__SELECTION,
                    Builder.directMapBuilder((Supplier<Map<String, Object>>) HashMap::new)
                        .set(map -> value -> map.put("windowHandles", value), windowHandles)
                        .get()));
        } else if (windowHandles.size() > 0) {
            makeFullscreenActionEngage.fire(
                new ActionEngage<>(windowHandles.get(0)));
        }
    }

    @FXML protected void terminate(ActionEvent event) {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO);
        confirmationAlert.initOwner(stage);
        confirmationAlert.setHeaderText(bundle.getString("stage.processList.confirmation.terminate"));
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
            errorMessage = Value.of(String.format(bundle.getString("stage.processList.error.unexpected"), thrown.getMessage()));
        }

        errorMessage.ifPresent(message -> {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setHeaderText(message);
            errorAlert.show();
        });
    }

}
