package name.wind.tools.process.browser;

import com.sun.jna.platform.win32.Win32Exception;
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
import name.wind.common.search.WildcardMultiphraseMatcher;
import name.wind.common.util.Builder;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.events.Action;
import name.wind.tools.process.browser.events.ActionEngage;
import name.wind.tools.process.browser.events.FXMLFormOpen;
import name.wind.tools.process.browser.events.FXMLLocation;
import name.wind.tools.process.browser.windows.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@ApplicationScoped @FXMLLocation(FXMLResources.FXML__PROCESS_LIST) public class ProcessListStageController
    extends StageController implements ResourceBundleAware, PreferencesAware {

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

        stage.setTitle(
            bundle.getString("stage.processList.title"));

        TreeItem<ExecutableHandle> processTreeRoot = new TreeItem<>(null);
        processTreeRoot.setExpanded(true);

        loadProcessTree(processTreeRoot, lastLoadedProcessHandles = ProcessRoutines.allAvailableProcesses().stream()
            .filter(retrievalResult -> retrievalResult.exception() == null)
            .map(ProcessHandleRetrievalResult::processHandle)
            .collect(
                toList()));
        processTreeTableView.setRoot(processTreeRoot);

        BooleanBinding selectionIsProcessHandle = Bindings.createBooleanBinding(
            () -> Value.of(processTreeTableView.getSelectionModel().getSelectedItem()).filter(selectedItem -> selectedItem != null && selectedItem.getValue() instanceof ProcessHandle).present(),
            processTreeTableView.getSelectionModel().selectedItemProperty());
        Stream.of(makeFullscreenMenuItem, terminateMenuItem).forEach(
            menuItem -> menuItem.disableProperty().bind(selectionIsProcessHandle.not()));

        String filterText = filterTextPreferencesEntry.get();

        if (filterText != null && filterText.trim().length() > 0) {
            applyFilter(filterText);
            filterTextField.setText(filterText);
            filterTextField.requestFocus();
        } else {
            processTreeTableView.requestFocus();
        }

        filterTextField.textProperty().addListener(this::filterTextChanged);
    }

    @Override protected Dimension2D preferredStageSize() {
        return Value.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 2, visualBounds.getHeight() / 2))
            .get();
    }

    private void filterTextChanged(ObservableValue<? extends String> property, String oldValue, String newValue) {
        applyFilter(newValue);
    }

    private void applyFilter(String filterText) {
        ExecutableHandleSearch.Continuation continuation = new ExecutableHandleSearch.Continuation();
        WildcardMultiphraseMatcher mather = new WildcardMultiphraseMatcher(filterText);

        processSearch.search(
            continuation,
            mather.toPredicate(object -> object instanceof ExecutableHandle
                ? ((ExecutableHandle) object).name()
                : ""),
            lastLoadedProcessHandles);

        loadProcessTree(processTreeTableView.getRoot(), continuation.searchResult());
        filterTextPreferencesEntry.accept(filterText);
    }

    private void loadProcessTree(TreeItem<ExecutableHandle> root, Collection<ProcessHandle> processHandles) {
        processTreeTableView.getSelectionModel().clearSelection(); // javafx bug
        root.getChildren().clear();

        processHandles
            .forEach(processInformation -> {
                TreeItem<ExecutableHandle> processItem = new TreeItem<>(processInformation);
                root.getChildren().add(processItem);

                processInformation.modules()
                    .forEach(processModuleInformation -> processItem.getChildren().add(
                        new TreeItem<>(processModuleInformation)));
            });

        processTreeTableView.sort();
    }

    @FXML protected void refresh(ActionEvent event) {
        lastLoadedProcessHandles = ProcessRoutines.allAvailableProcesses().stream()
            .filter(retrievalResult -> retrievalResult.exception() == null)
            .map(ProcessHandleRetrievalResult::processHandle)
            .collect(
                toList());
        applyFilter(filterTextField.getText());
    }

    @FXML protected void run(ActionEvent event) {
        fxmlFormOpenEvent.fire(
            new FXMLFormOpen(
                Builder.direct(Stage::new)
                    .set(target -> target::initOwner, stage)
                    .set(target -> target::initModality, Modality.APPLICATION_MODAL)
                    .set(target -> target::setResizable, false)
                    .get(),
                FXMLResources.FXML__RUN,
                emptyMap()));
    }

    @FXML protected void makeFullscreen(ActionEvent event) {
        TreeItem<ExecutableHandle> selectedItem = processTreeTableView.getSelectionModel().getSelectedItem();

        List<WindowHandle> windowHandles = WindowRoutines.processWindowHandles(
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

        Alert confirmationAlert = prepareAlert(() -> new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO));
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
            Alert errorAlert = prepareAlert(() -> new Alert(Alert.AlertType.ERROR));
            errorAlert.setHeaderText(message);
            errorAlert.show();
        });
    }

}
