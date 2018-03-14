package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.application.annotation.Action;
import com.github.windchopper.common.fx.application.fx.annotation.FXMLResource;
import com.github.windchopper.common.fx.application.fx.event.ActionEngage;
import com.github.windchopper.common.fx.application.fx.event.FXMLResourceOpen;
import com.github.windchopper.common.util.KnownSystemProperties;
import com.github.windchopper.common.util.Pipeliner;
import com.github.windchopper.tools.process.browser.jna.WindowHandle;
import com.github.windchopper.tools.process.browser.jna.WindowRoutines;
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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.binarySearch;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@ApplicationScoped @FXMLResource(FXMLResources.FXML__PROCESS_LIST) public class ProcessListStageController
    extends AnyStageController implements PreferencesAware {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

    private class AutoRefreshThread extends Thread {

        public AutoRefreshThread() {
            setDaemon(true);
        }

        @Override public void run() {
            while (true) {
                try {
                    Thread.sleep(Duration.ofSeconds(5).toMillis());
                    if (toggleAutoRefreshMenuItem.isSelected()) Platform.runLater(ProcessListStageController.this::refreshImpl);
                } catch (InterruptedException thrown) {
                    break;
                }
            }
        }

    }

    @Inject protected Event<FXMLResourceOpen> fxmlFormOpenEvent;
    @Inject @Action("makeFullscreen") protected Event<ActionEngage<WindowHandle>> makeFullscreenActionEngage;

    @FXML protected TableView<ProcessHandleRepresentative> processTableView;
    @FXML protected TextField filterTextField;
    @FXML protected MenuItem refreshMenuItem;
    @FXML protected MenuItem makeFullscreenMenuItem;
    @FXML protected MenuItem terminateMenuItem;
    @FXML protected CheckMenuItem toggleAutoRefreshMenuItem;

    private List<ProcessHandleRepresentative> lastLoadedProcessHandles;

    @Override protected void start(Stage stage, String fxmlResource, Map<String, ?> parameters) {
        super.start(
            Pipeliner.of(() -> stage)
                .set(target -> target::setTitle, bundle.getString("stage.processList.title"))
                .get(),
            fxmlResource,
            parameters);

        loadProcessTree(lastLoadedProcessHandles = ProcessHandle.allProcesses()
            .map(ProcessHandleRepresentative::new).sorted(comparing(ProcessHandleRepresentative::pid)).collect(
                toList()));

        BooleanBinding selectionIsProcessHandle = Bindings.isNotNull(
            processTableView.getSelectionModel().selectedItemProperty());
        Stream.of(makeFullscreenMenuItem, terminateMenuItem).forEach(
            menuItem -> menuItem.disableProperty().bind(selectionIsProcessHandle.not()));

        String filterText = filterTextPreferencesEntry.get();

        if (filterText != null && filterText.trim().length() > 0) {
            applyFilter(filterText);
            filterTextField.setText(filterText);
            filterTextField.requestFocus();
        } else {
            processTableView.requestFocus();
        }

        filterTextField.textProperty().addListener(this::filterTextChanged);
        refreshMenuItem.disableProperty().bind(toggleAutoRefreshMenuItem.selectedProperty());
        toggleAutoRefreshMenuItem.setSelected(Optional.ofNullable(autoRefreshPreferencesEntry.get())
            .orElse(false));

        new AutoRefreshThread().start();
    }

    @Override protected Dimension2D preferredStageSize() {
        return Pipeliner.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 2, visualBounds.getHeight() / 2))
            .get();
    }

    private void filterTextChanged(@SuppressWarnings("unused") ObservableValue<? extends String> property, @SuppressWarnings("unused") String oldValue, String newValue) {
        applyFilter(newValue);
    }

    private boolean matches(ProcessHandleRepresentative handle, String filterText) {
        Pattern pattern = Pattern.compile(filterText);
        return pattern.matcher(Optional.ofNullable(handle).map(ProcessHandleRepresentative::name).orElse("")).find()
            || pattern.matcher(Optional.ofNullable(handle).map(ProcessHandleRepresentative::name).map(Object::toString).orElse("")).find();
    }

    private void applyFilter(String filterText) {
        filterTextPreferencesEntry.accept(filterText);
        loadProcessTree(lastLoadedProcessHandles.stream()
            .filter(handle -> matches(handle, filterText))
            .sorted(comparing(ProcessHandleRepresentative::pid))
            .collect(toList()));
    }

    private void loadProcessTree(Collection<ProcessHandleRepresentative> processHandles) {
        long[] selectedPids = processTableView.getSelectionModel().getSelectedItems().stream()
            .mapToLong(ProcessHandleRepresentative::pid).toArray();

        processTableView.getSelectionModel().clearSelection(); // javafx bug
        processTableView.getItems().clear();
        processTableView.getItems().addAll(processHandles);
        processTableView.sort();

        processHandles.forEach(handle -> {
            if (binarySearch(selectedPids, handle.pid()) >= 0) {
                processTableView.getSelectionModel().select(handle);
            }
        });
    }

    protected void refreshImpl() {
        lastLoadedProcessHandles = ProcessHandle.allProcesses()
            .map(ProcessHandleRepresentative::new).collect(
                toList());
        applyFilter(filterTextField.getText());
    }

    @FXML protected void refresh(ActionEvent event) {
        refreshImpl();
    }

    @FXML protected void toggleAutoRefresh(ActionEvent event) {
        autoRefreshPreferencesEntry.accept(toggleAutoRefreshMenuItem.isSelected());
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
        boolean allowed = KnownSystemProperties.operationSystemName.get()
            .filter(name -> name.toLowerCase().contains("windows"))
            .isPresent();

        if (allowed) {
            ProcessHandleRepresentative selectedItem = processTableView.getSelectionModel().getSelectedItem();

            List<WindowHandle> windowHandles = WindowRoutines.processWindowHandles(
                selectedItem.pid());

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
        } else {
            Pipeliner.of(prepareAlert(() -> new Alert(Alert.AlertType.ERROR)))
                .set(alert -> alert::setHeaderText, bundle.getString("stage.processList.error.notWindows"))
                .accept(Alert::show);
        }
    }

    @FXML protected void terminate(ActionEvent event) {
        ProcessHandleRepresentative selectedItem = processTableView.getSelectionModel().getSelectedItem();

        boolean terminate = Pipeliner.of(prepareAlert(() -> new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO)))
            .set(alert -> alert::setHeaderText, bundle.getString("stage.processList.confirmation.terminate"))
            .map(Alert::showAndWait)
            .get()
            .map(choice -> choice == ButtonType.YES)
            .orElse(false);

        try {
            if (terminate) {
                selectedItem.destroyForcibly();
                processTableView.getItems().remove(selectedItem);
            }
        } catch (Exception thrown) {
            Pipeliner.of(prepareAlert(() -> new Alert(Alert.AlertType.ERROR)))
                .set(alert -> alert::setHeaderText, String.format(bundle.getString("stage.processList.error.unexpected"), thrown.getMessage()))
                .accept(Alert::show);
        }
    }

}
