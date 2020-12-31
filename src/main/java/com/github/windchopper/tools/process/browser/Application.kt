package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.cdi.ResourceBundleLoad
import com.github.windchopper.common.fx.cdi.form.StageFormLoad
import com.github.windchopper.common.preferences.BufferedPreferencesEntry
import com.github.windchopper.common.preferences.PlatformPreferencesStorage
import com.github.windchopper.common.preferences.PreferencesStorage
import com.github.windchopper.common.preferences.types.FlatType
import com.github.windchopper.common.util.ClassPathResource
import jakarta.enterprise.inject.spi.CDI
import javafx.scene.control.Alert
import javafx.stage.Stage
import org.jboss.weld.environment.se.Weld
import java.io.File
import java.time.Duration
import java.util.*
import java.util.prefs.Preferences

fun <T> T.display(stageController: AnyStageController) where T: Throwable {
    stageController.prepareAlert(Alert.AlertType.ERROR, message)
        .show()
}

fun AnyStageController.exceptionally(runnable: () -> Unit): AnyStageController {
    try {
        runnable.invoke()
    } catch (thrown: Exception) {
        thrown.display(this)
    }

    return this
}

fun String.trimToNull(): String? = trim().let {
    if (it.isNotEmpty()) it else null
}

fun <T> T.identity(): T = this

class Application: javafx.application.Application() {

    companion object {

        const val FXML__PROCESS_LIST = "com/github/windchopper/tools/process/browser/processListStage.fxml"
        const val FXML__SELECTION = "com/github/windchopper/tools/process/browser/selectionStage.fxml"
        const val FXML__RUN = "com/github/windchopper/tools/process/browser/runStage.fxml"

        private val resourceBundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages")

        val messages = resourceBundle.keySet()
            .map { it to resourceBundle.getString(it) }
            .toMap()

        private val defaultBufferLifetime = Duration.ofMinutes(1)
        private val preferencesStorage: PreferencesStorage = PlatformPreferencesStorage(Preferences.userRoot().node("com/github/windchopper/tools/process/browser"))

        val filterTextPreferencesEntry = BufferedPreferencesEntry(preferencesStorage, "filterText", FlatType(String::identity, String::identity), defaultBufferLifetime)
        val browseInitialDirectoryPreferencesEntry = BufferedPreferencesEntry(preferencesStorage, "browseInitialDirectory", FlatType(::File, File::getAbsolutePath), defaultBufferLifetime)
        val autoRefreshPreferencesEntry = BufferedPreferencesEntry(preferencesStorage, "autoRefresh", FlatType(String::toBoolean, Boolean::toString), defaultBufferLifetime)

        fun main(args: Array<String>) {
            launch(*args)
        }

    }

    private lateinit var weld: Weld

    override fun init() {
        weld = Weld().let {
            it.initialize()
            it
        }
    }

    override fun stop() {
        weld.shutdown()
    }

    override fun start(primaryStage: Stage) {
        with (CDI.current().beanManager) {
            fireEvent(ResourceBundleLoad(resourceBundle))
            fireEvent(StageFormLoad(ClassPathResource(FXML__PROCESS_LIST)) { primaryStage })
        }
    }

}