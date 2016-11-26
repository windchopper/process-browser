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
import name.wind.application.cdi.annotation.Action;
import name.wind.application.cdi.fx.annotation.FXMLResource;
import name.wind.application.cdi.fx.event.ActionEngage;
import name.wind.application.cdi.fx.event.FXMLResourceOpen;
import name.wind.common.util.HierarchyIterator;
import name.wind.common.util.Pipeliner;
import name.wind.tools.process.browser.windows.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

@ApplicationScoped @FXMLResource(FXMLResources.FXML__PROCESS_LIST) public class ProcessListStageController
    extends AnyStageController implements ResourceBundleAware, PreferencesAware {

    @Inject protected Event<FXMLResourceOpen> fxmlFormOpenEvent;
    @Inject @Action("makeFullscreen") protected Event<ActionEngage<WindowHandle>> makeFullscreenActionEngage;

    @FXML protected TreeTableView<ExecutableHandle> processTreeTableView;
    @FXML protected TextField filterTextField;
    @FXML protected MenuItem makeFullscreenMenuItem;
    @FXML protected MenuItem terminateMenuItem;

    private List<ProcessHandle> lastLoadedProcessHandles;

    @Override protected void start(Stage stage, String fxmlResource, Map<String, ?> parameters) {
        super.start(
            Pipeliner.of(() -> stage)
                .set(target -> target::setTitle, bundle.getString("stage.processList.title"))
                .get(),
            fxmlResource,
            parameters);

        TreeItem<ExecutableHandle> processTreeRoot = new TreeItem<>(null);
        processTreeRoot.setExpanded(true);

        loadProcessTree(processTreeRoot, lastLoadedProcessHandles = ProcessRoutines.allAvailableProcesses().stream()
            .filter(retrievalResult -> retrievalResult.exception() == null)
            .map(ProcessHandleRetrievalResult::processHandle)
            .collect(
                toList()));
        processTreeTableView.setRoot(processTreeRoot);

        BooleanBinding selectionIsProcessHandle = Bindings.createBooleanBinding(
            () -> Optional.of(processTreeTableView.getSelectionModel().getSelectedItem()).filter(selectedItem -> selectedItem != null && selectedItem.getValue() instanceof ProcessHandle).isPresent(),
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
        return Pipeliner.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 2, visualBounds.getHeight() / 2))
            .get();
    }

    private void filterTextChanged(ObservableValue<? extends String> property, String oldValue, String newValue) {
        applyFilter(newValue);
    }

    private boolean matches(TreeItem<ExecutableHandle> item, String filterText) {
        Pattern pattern = Pattern.compile(filterText);
        return pattern.matcher(item.getValue().name()).find()
            || pattern.matcher(item.getValue().path().toString()).find();
    }

    private void applyFilter(String filterText) {
        filterTextPreferencesEntry.accept(filterText);
        loadProcessTree(processTreeTableView.getRoot(), StreamSupport.stream(Spliterators.spliterator(
                new HierarchyIterator<>(processTreeTableView.getRoot(), TreeItem::getChildren), 0, Spliterator.IMMUTABLE), false)
            .filter(item -> matches(item, filterText) && item.getValue() instanceof ProcessHandle)
            .map(item -> (ProcessHandle) item.getValue())
            .collect(toList()));
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
            new FXMLResourceOpen(
                Pipeliner.of(Stage::new)
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
                new FXMLResourceOpen(
                    Pipeliner.of(Stage::new)
                        .set(target -> target::initOwner, stage)
                        .set(target -> target::initModality, Modality.APPLICATION_MODAL)
                        .set(target -> target::setResizable, false)
                        .get(),
                    FXMLResources.FXML__SELECTION,
                    Pipeliner.of((Supplier<Map<String, Object>>) HashMap::new)
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

        Optional<String> errorMessage = Optional.empty();

        try {
            if (terminate) {
                ProcessHandle processHandle = (ProcessHandle) selectedItem.getValue();
                processHandle.terminate(0);

                selectedItem.getParent().getChildren().remove(selectedItem);
            }
        } catch (Win32Exception thrown) {
            errorMessage = Optional.of(String.format(bundle.getString("stage.processList.error.unexpected"), thrown.getMessage()));
        }

        errorMessage.ifPresent(message -> {
            Alert errorAlert = prepareAlert(() -> new Alert(Alert.AlertType.ERROR));
            errorAlert.setHeaderText(message);
            errorAlert.show();
        });
    }

}
