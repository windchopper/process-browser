package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.cdi.form.Form
import com.github.windchopper.common.fx.cdi.form.FormController
import com.github.windchopper.common.fx.cdi.form.StageFormLoad
import com.github.windchopper.common.util.ClassPathResource
import com.github.windchopper.tools.process.browser.MakeFullScreenPerformer.MakeFullScreen
import com.github.windchopper.tools.process.browser.WindowInfo.Companion.allWindowsOf
import com.github.windchopper.tools.process.browser.WindowInfo.Companion.available
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Dimension2D
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.stage.Modality
import javafx.stage.Screen
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.Comparator.comparing
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Level
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Event
import javax.inject.Inject
import kotlin.streams.toList

@ApplicationScoped @Form(Application.FXML__PROCESS_LIST) class ProcessListStageController: AnyStageController() {

    @Inject protected lateinit var fxmlFormOpenEvent: Event<StageFormLoad>
    @Inject protected lateinit var makeFullscreenEvent: Event<MakeFullScreen>

    @FXML protected lateinit var processTableView: TableView<ProcessInfo>
    @FXML protected lateinit var filterTextField: TextField
    @FXML protected lateinit var refreshMenuItem: MenuItem
    @FXML protected lateinit var makeFullscreenMenuItem: MenuItem
    @FXML protected lateinit var terminateMenuItem: MenuItem
    @FXML protected lateinit var toggleAutoRefreshMenuItem: CheckMenuItem

    private var processList: List<ProcessInfo>? = null

    override fun preferredStageSize(): Dimension2D {
        return Screen.getPrimary().visualBounds
            .let { Dimension2D(it.width / 2, it.height / 2) }
    }

    override fun afterLoad(form: Parent, parameters: Map<String, *>, formNamespace: Map<String, *>) {
        super.afterLoad(form, parameters, formNamespace)

        stage.title = Application.messages.getString("stage.processList.title")

        loadProcessTree(loadProcessList()
            .also { processList = it })

        val selectionIsProcessInfo = Bindings.isNotNull(processTableView.selectionModel.selectedItemProperty())

        listOf(makeFullscreenMenuItem, terminateMenuItem)
            .forEach { it.disableProperty().bind(selectionIsProcessInfo.not()) }

        listOf(makeFullscreenMenuItem, terminateMenuItem)
            .forEach { it.disableProperty().bind(selectionIsProcessInfo.not()) }

        val filterText = Application.filterTextPreferencesEntry.load()
            ?.trimToNull()

        if (filterText == null) {
            processTableView.requestFocus()
        } else {
            applyFilter(filterText)
            filterTextField.text = filterText
            filterTextField.requestFocus()
        }

        filterTextField.textProperty().addListener { property, oldValue, newValue -> filterTextChanged(property, oldValue, newValue) }
        toggleAutoRefreshMenuItem.isSelected = Application.autoRefreshPreferencesEntry.load()?:false
        refreshMenuItem.disableProperty().bind(toggleAutoRefreshMenuItem.selectedProperty())

        GlobalScope.launch {
            val timeout = Duration.ofSeconds(5).toMillis()

            while (isActive) {
                delay(timeout)

                if (toggleAutoRefreshMenuItem.isSelected) {
                    Platform.runLater { refreshImpl() }
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER") private fun filterTextChanged(property: ObservableValue<*>, oldValue: String?, newValue: String?) {
        applyFilter(newValue)
    }

    private fun matches(processInfo: ProcessInfo, filterText: String?): Boolean {
        return try {
            filterText
                ?.let { Pattern.compile(it) }
                ?.let { it.matcher(processInfo.name).find() || it.matcher(processInfo.command).find() }
                ?:false
        } catch (thrown: PatternSyntaxException) {
            if (FormController.logger.isLoggable(Level.FINER)) {
                FormController.logger.log(Level.FINER, thrown.message, thrown)
            }

            false
        }
    }

    private fun applyFilter(filterText: String?) {
        Application.filterTextPreferencesEntry.save(filterText)
        loadProcessTree(processList!!.stream()
            .filter { matches(it, filterText) }
            .sorted(comparing(ProcessInfo::pid))
            .toList())
    }

    private fun loadProcessTree(processHandles: Collection<ProcessInfo>) {
        val selectedPids = processTableView.selectionModel.selectedItems
            .map { it.pid }.toTypedArray()

        processTableView.selectionModel.clearSelection() // javafx bug
        processTableView.items.clear()
        processTableView.items.addAll(processHandles)
        processTableView.sort()

        processHandles.forEach(Consumer {
            if (selectedPids.contains(it.pid)) {
                processTableView.selectionModel.select(it)
            }
        })
    }

    private fun loadProcessList(): List<ProcessInfo> {
        return ProcessHandle.allProcesses()
            .filter { it.info().command().isPresent }
            .map { ProcessInfo(it) }
            .toList()
    }

    private fun refreshImpl() {
        processList = loadProcessList()
        applyFilter(filterTextField.text)
    }

    @FXML @Suppress("UNUSED_PARAMETER") protected fun refresh(event: ActionEvent) {
        refreshImpl()
    }

    @FXML @Suppress("UNUSED_PARAMETER") protected fun toggleAutoRefresh(event: ActionEvent) {
        Application.autoRefreshPreferencesEntry.save(toggleAutoRefreshMenuItem.isSelected)
    }

    @FXML @Suppress("UNUSED_PARAMETER") protected fun run(event: ActionEvent) {
        fxmlFormOpenEvent.fire(StageFormLoad(
            ClassPathResource(Application.FXML__RUN),
            Supplier {
                Stage().let {
                    it.initOwner(stage)
                    it.initModality(Modality.APPLICATION_MODAL)
                    it.isResizable = false
                    it
                }
            }))
    }

    @FXML
    protected fun makeFullscreen(event: ActionEvent?) {
        if (available()) {
            val windowHandles = allWindowsOf(processTableView.selectionModel.selectedItem.pid)

            when {
                windowHandles.size > 1 -> {
                    fxmlFormOpenEvent.fire(StageFormLoad(
                        ClassPathResource(Application.FXML__SELECTION),
                        mapOf("windowHandles" to windowHandles),
                        Supplier {
                            Stage().let {
                                it.initOwner(stage)
                                it.initModality(Modality.APPLICATION_MODAL)
                                it.isResizable = false
                                it
                            }
                        }))
                }
                windowHandles.isNotEmpty() -> {
                    makeFullscreenEvent.fire(MakeFullScreen(this, windowHandles.first()))
                }
                else -> {
                    prepareAlert(AlertType.ERROR, Application.messages.getString("stage.processList.error.noWindows"))
                        .show()
                }
            }
        } else {
            prepareAlert(AlertType.ERROR, Application.messages.getString("stage.processList.error.operatingSystemNotSupported"))
                .show()
        }
    }

    @FXML protected fun terminate(event: ActionEvent?) {
        val selectedItem = processTableView.selectionModel.selectedItem
        val terminate = prepareAlert(AlertType.CONFIRMATION, Application.messages.getString("stage.processList.confirmation.terminate"), ButtonType.YES, ButtonType.NO)
            .showAndWait().filter { it == ButtonType.YES }.isPresent

        try {
            if (terminate) {
                selectedItem.destroyForcibly()
                processTableView.items.remove(selectedItem)
            }
        } catch (thrown: Exception) {
            thrown.display(this)
        }
    }

}