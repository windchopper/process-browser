package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.form.Form;
import com.github.windchopper.common.fx.cdi.form.StageFormLoad;
import com.github.windchopper.common.util.ClassPathResource;
import com.github.windchopper.common.util.Pipeliner;
import com.github.windchopper.tools.process.browser.MakeFullScreenPerformer.MakeFullScreen;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Dimension2D;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static java.util.Arrays.binarySearch;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@ApplicationScoped @Form(Application.FXML__PROCESS_LIST) public class ProcessListStageController extends AnyStageController {

    private class AutoRefreshThread extends Thread {

        public AutoRefreshThread() {
            setDaemon(true);
        }

        @Override @SuppressWarnings("BusyWait") public void run() {
            try {
                long timeout = Duration.ofSeconds(5).toMillis();

                do {
                    Thread.sleep(timeout);
                    if (toggleAutoRefreshMenuItem.isSelected()) {
                        Platform.runLater(ProcessListStageController.this::refreshImpl);
                    }
                } while (
                    !Thread.interrupted());
            } catch (InterruptedException ignored) {
            }
        }

    }

    @Inject protected Event<StageFormLoad> fxmlFormOpenEvent;
    @Inject protected Event<MakeFullScreen> makeFullscreenEvent;

    @FXML protected TableView<ProcessInfo> processTableView;
    @FXML protected TextField filterTextField;
    @FXML protected MenuItem refreshMenuItem;
    @FXML protected MenuItem makeFullscreenMenuItem;
    @FXML protected MenuItem terminateMenuItem;
    @FXML protected CheckMenuItem toggleAutoRefreshMenuItem;

    private List<ProcessInfo> processList;

    @Override protected void afterLoad(Parent form, Map<String, ?> parameters, Map<String, ?> formNamespace) {
        super.afterLoad(form, parameters, formNamespace);
        stage.setTitle(Application.messages.getString("stage.processList.title"));

        loadProcessTree(processList = loadProcessList());

        var selectionIsProcessHandle = Bindings.isNotNull(
            processTableView.getSelectionModel().selectedItemProperty());
        Stream.of(makeFullscreenMenuItem, terminateMenuItem).forEach(
            menuItem -> menuItem.disableProperty().bind(selectionIsProcessHandle.not()));

        var filterText = Application.filterTextPreferencesEntry.load();

        if (filterText != null && filterText.trim().length() > 0) {
            applyFilter(filterText);
            filterTextField.setText(filterText);
            filterTextField.requestFocus();
        } else {
            processTableView.requestFocus();
        }

        filterTextField.textProperty().addListener(this::filterTextChanged);
        refreshMenuItem.disableProperty().bind(toggleAutoRefreshMenuItem.selectedProperty());
        toggleAutoRefreshMenuItem.setSelected(Optional.ofNullable(Application.autoRefreshPreferencesEntry.load())
            .orElse(false));

        new AutoRefreshThread()
            .start();
    }

    @Override protected Dimension2D preferredStageSize() {
        return Pipeliner.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 2, visualBounds.getHeight() / 2))
            .get();
    }

    @SuppressWarnings("unused") private void filterTextChanged(ObservableValue<? extends String> property, String oldValue, String newValue) {
        applyFilter(newValue);
    }

    private boolean matches(ProcessInfo handle, String filterText) {
        try {
            return Pipeliner.of(filterText)
                .map(Pattern::compile)
                .map(pattern -> pattern.matcher(handle.name()).find() || pattern.matcher(handle.command()).find())
                .get();
        } catch (PatternSyntaxException thrown) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, thrown.getMessage(), thrown);
            }

            return false;
        }
    }

    private void applyFilter(String filterText) {
        Application.filterTextPreferencesEntry.save(filterText);
        loadProcessTree(processList.stream()
            .filter(handle -> matches(handle, filterText))
            .sorted(comparing(ProcessInfo::pid))
            .collect(toList()));
    }

    private void loadProcessTree(Collection<ProcessInfo> processHandles) {
        var selectedPids = processTableView.getSelectionModel().getSelectedItems().stream()
            .mapToLong(ProcessInfo::pid).toArray();

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

    private List<ProcessInfo> loadProcessList() {
        return ProcessHandle.allProcesses()
            .filter(handle -> handle.info().command().isPresent()).map(ProcessInfo::new).collect(
                toList());
    }

    protected void refreshImpl() {
        processList = loadProcessList();
        applyFilter(filterTextField.getText());
    }

    @FXML protected void refresh(ActionEvent event) {
        refreshImpl();
    }

    @FXML protected void toggleAutoRefresh(ActionEvent event) {
        Application.autoRefreshPreferencesEntry.save(toggleAutoRefreshMenuItem.isSelected());
    }

    @FXML protected void run(ActionEvent event) {
        fxmlFormOpenEvent.fire(new StageFormLoad(
            new ClassPathResource(Application.FXML__RUN),
            Pipeliner.of(Stage::new)
                .set(target -> target::initOwner, stage)
                .set(target -> target::initModality, Modality.APPLICATION_MODAL)
                .set(target -> target::setResizable, false)));
    }

    @FXML protected void makeFullscreen(ActionEvent event) {
        if (WindowInfoFactory.available()) {
            var windowHandles = WindowInfoFactory.allWindowsOf(
                processTableView.getSelectionModel().getSelectedItem().pid());

            if (windowHandles.size() > 1) {
                fxmlFormOpenEvent.fire(new StageFormLoad(
                    new ClassPathResource(Application.FXML__SELECTION),
                    Map.of("windowHandles", windowHandles),
                    Pipeliner.of(Stage::new)
                        .set(target -> target::initOwner, stage)
                        .set(target -> target::initModality, Modality.APPLICATION_MODAL)
                        .set(target -> target::setResizable, false)));
            } else if (windowHandles.size() > 0) {
                makeFullscreenEvent.fire(new MakeFullScreen(this, windowHandles.get(0)));
            }
        } else {
            prepareAlert(Alert.AlertType.ERROR)
                .set(bean -> bean::setHeaderText, Application.messages.getString("stage.processList.error.operatingSystemNotSupported"))
                .get().show();
        }
    }

    @FXML protected void terminate(ActionEvent event) {
        var selectedItem = processTableView.getSelectionModel().getSelectedItem();

        var terminate = prepareAlert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO)
            .set(alert -> alert::setHeaderText, Application.messages.getString("stage.processList.confirmation.terminate"))
            .map(Alert::showAndWait)
            .get().map(choice -> choice == ButtonType.YES)
            .orElse(false);

        try {
            if (terminate) {
                selectedItem.destroyForcibly();
                processTableView.getItems().remove(selectedItem);
            }
        } catch (Exception thrown) {
            prepareAlert(Alert.AlertType.ERROR)
                .set(bean -> bean::setHeaderText, String.format(Application.messages.getString("stage.processList.error.unexpected"), thrown.getMessage()))
                .get().show();
        }
    }

}
