package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.cdi.form.Form
import com.github.windchopper.common.fx.cdi.form.FormController
import com.github.windchopper.common.fx.cdi.form.StageFormLoad
import com.github.windchopper.common.util.ClassPathResource
import com.github.windchopper.tools.process.browser.WindowInfo.Companion.allWindowsOf
import javafx.application.Platform
import javafx.beans.binding.Bindings
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
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Level
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Event
import javax.inject.Inject

@ApplicationScoped @Form(Application.FXML__PROCESS_LIST) class ProcessListStageController: AnyStageController() {

    @Inject protected lateinit var fxmlFormOpenEvent: Event<StageFormLoad>

    @FXML protected lateinit var processTableView: TableView<ProcessInfo>
    @FXML protected lateinit var filterTextField: TextField
    @FXML protected lateinit var refreshMenuItem: MenuItem
    @FXML protected lateinit var makeFullscreenMenuItem: MenuItem
    @FXML protected lateinit var terminateMenuItem: MenuItem
    @FXML protected lateinit var toggleAutoRefreshMenuItem: CheckMenuItem

    private val processInfoList: MutableList<ProcessInfo> = ArrayList()

    override fun preferredStageSize(): Dimension2D {
        return Screen.getPrimary().visualBounds
            .let { Dimension2D(it.width / 2, it.height / 2) }
    }

    override fun afterLoad(form: Parent, parameters: Map<String, *>, formNamespace: Map<String, *>) {
        super.afterLoad(form, parameters, formNamespace)

        stage.title = Application.messages["stage.processList.title"]

        loadProcessTableView(reloadProcessInfoList())

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

        filterTextField.textProperty().addListener { property, oldValue, newValue -> applyFilter(newValue) }
        toggleAutoRefreshMenuItem.isSelected = Application.autoRefreshPreferencesEntry.load()?:false
        refreshMenuItem.disableProperty().bind(toggleAutoRefreshMenuItem.selectedProperty())

        GlobalScope.launch {
            val timeout = Duration.ofSeconds(5).toMillis()

            while (isActive) {
                delay(timeout)

                if (toggleAutoRefreshMenuItem.isSelected) {
                    Platform.runLater {
                        loadProcessTableView(reloadProcessInfoList())
                        applyFilter(filterTextField.text)
                    }
                }
            }
        }
    }

    private fun matches(processInfo: ProcessInfo, filterText: String?): Boolean {
        return try {
            filterText
                ?.replace("*", ".*?")
                ?.replace("?", ".")
                ?.replace("\\", "\\\\")
                ?.let { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }
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
        loadProcessTableView(processInfoList
            .filter { matches(it, filterText) }
            .sortedBy { it.pid }
            .toList())
    }

    private fun loadProcessTableView(processInfoList: Collection<ProcessInfo>) {
        with (processTableView) {
            val selectedPids = selectionModel.selectedItems
                .map { it.pid }.toTypedArray()

            selectionModel.clearSelection() // javafx bug
            items.clear()
            items.addAll(processInfoList)
            sort()

            processInfoList.forEach(Consumer {
                if (selectedPids.contains(it.pid)) {
                    selectionModel.select(it)
                }
            })
        }
    }

    private fun reloadProcessInfoList(): List<ProcessInfo> {
        with (processInfoList) {
            clear()

            ProcessHandle.allProcesses()
                .filter { it.info().command().isPresent }
                .map { ProcessInfo(it) }
                .forEach { processInfoList.add(it) }

            return this
        }
    }

    @FXML @Suppress("UNUSED_PARAMETER") protected fun refresh(event: ActionEvent) {
        loadProcessTableView(reloadProcessInfoList())
        applyFilter(filterTextField.text)
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
        exceptionally {
            with (allWindowsOf(processTableView.selectionModel.selectedItem.pid)) {
                when {
                    size > 1 -> {
                        fxmlFormOpenEvent.fire(StageFormLoad(
                            ClassPathResource(Application.FXML__SELECTION),
                            mapOf("windowHandles" to this),
                            Supplier {
                                Stage().let {
                                    it.initOwner(stage)
                                    it.initModality(Modality.APPLICATION_MODAL)
                                    it.isResizable = false
                                    it
                                }
                            }))
                    }
                    isNotEmpty() -> {
                        exceptionally {
                            this.first().makeFullScreen()
                        }
                    }
                    else -> {
                        prepareAlert(AlertType.ERROR, Application.messages["stage.processList.error.noWindowsFound"])
                            .show()
                    }
                }
            }
        }
    }

    @FXML protected fun terminate(event: ActionEvent?) {
        val selectedItem = processTableView.selectionModel.selectedItem
        val terminate = prepareAlert(AlertType.CONFIRMATION, Application.messages["stage.processList.confirmation.terminate"], ButtonType.YES, ButtonType.NO)
            .showAndWait().filter { it == ButtonType.YES }.isPresent

        exceptionally {
            if (terminate) {
                selectedItem.destroyForcibly()
                processTableView.items.remove(selectedItem)
            }
        }
    }

}