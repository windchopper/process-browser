package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.Action;
import com.github.windchopper.common.fx.cdi.ActionEngage;
import com.github.windchopper.common.fx.cdi.form.Form;
import com.github.windchopper.common.fx.cdi.form.StageFormLoad;
import com.github.windchopper.common.util.ClassPathResource;
import com.github.windchopper.common.util.Pipeliner;
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
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static java.util.Arrays.binarySearch;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@ApplicationScoped @Form(FXMLResources.FXML__PROCESS_LIST) public class ProcessListStageController extends AnyStageController implements PreferencesAware {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

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
    @Inject @Action("makeFullscreen") protected Event<ActionEngage<WindowInfo<?>>> makeFullscreenActionEngage;

    @FXML protected TableView<ProcessInfo> processTableView;
    @FXML protected TextField filterTextField;
    @FXML protected MenuItem refreshMenuItem;
    @FXML protected MenuItem makeFullscreenMenuItem;
    @FXML protected MenuItem terminateMenuItem;
    @FXML protected CheckMenuItem toggleAutoRefreshMenuItem;

    private List<ProcessInfo> processList;

    @Override protected void afterLoad(Parent form, Map<String, ?> parameters, Map<String, ?> formNamespace) {
        super.afterLoad(form, parameters, formNamespace);
        stage.setTitle(bundle.getString("stage.processList.title"));

        loadProcessTree(processList = loadProcessList());

        var selectionIsProcessHandle = Bindings.isNotNull(
            processTableView.getSelectionModel().selectedItemProperty());
        Stream.of(makeFullscreenMenuItem, terminateMenuItem).forEach(
            menuItem -> menuItem.disableProperty().bind(selectionIsProcessHandle.not()));

        var filterText = filterTextPreferencesEntry.load();

        if (filterText != null && filterText.trim().length() > 0) {
            applyFilter(filterText);
            filterTextField.setText(filterText);
            filterTextField.requestFocus();
        } else {
            processTableView.requestFocus();
        }

        filterTextField.textProperty().addListener(this::filterTextChanged);
        refreshMenuItem.disableProperty().bind(toggleAutoRefreshMenuItem.selectedProperty());
        toggleAutoRefreshMenuItem.setSelected(Optional.ofNullable(autoRefreshPreferencesEntry.load())
            .orElse(false));

        new AutoRefreshThread()
            .start();
    }

    @Override protected Dimension2D preferredStageSize() {
        return Pipeliner.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 2, visualBounds.getHeight() / 2))
            .get();
    }

    private void filterTextChanged(@SuppressWarnings("unused") ObservableValue<? extends String> property, @SuppressWarnings("unused") String oldValue, String newValue) {
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
        filterTextPreferencesEntry.save(filterText);
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
        autoRefreshPreferencesEntry.save(toggleAutoRefreshMenuItem.isSelected());
    }

    @FXML protected void run(ActionEvent event) {
        fxmlFormOpenEvent.fire(new StageFormLoad(
            new ClassPathResource(FXMLResources.FXML__RUN),
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
                    new ClassPathResource(FXMLResources.FXML__SELECTION),
                    Map.of("windowHandles", windowHandles),
                    Pipeliner.of(Stage::new)
                        .set(target -> target::initOwner, stage)
                        .set(target -> target::initModality, Modality.APPLICATION_MODAL)
                        .set(target -> target::setResizable, false)));
            } else if (windowHandles.size() > 0) {
                makeFullscreenActionEngage.fire(new ActionEngage<>(
                    windowHandles.get(0)));
            }
        } else {
            Pipeliner.of(prepareAlert(() -> new Alert(Alert.AlertType.ERROR)))
                .set(alert -> alert::setHeaderText, bundle.getString("stage.processList.error.operatingSystemNotSupported"))
                .accept(Alert::show);
        }
    }

    @FXML protected void terminate(ActionEvent event) {
        var selectedItem = processTableView.getSelectionModel().getSelectedItem();

        var terminate = Pipeliner.of(prepareAlert(() -> new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO)))
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
