package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.cdi.ResourceBundleLoad
import com.github.windchopper.common.fx.cdi.form.StageFormLoad
import com.github.windchopper.common.preferences.PlatformPreferencesStorage
import com.github.windchopper.common.preferences.PreferencesEntry
import com.github.windchopper.common.preferences.PreferencesStorage
import com.github.windchopper.common.preferences.types.FlatType
import com.github.windchopper.common.util.ClassPathResource
import javafx.scene.control.Alert
import javafx.stage.Stage
import org.jboss.weld.environment.se.Weld
import java.io.File
import java.time.Duration
import java.util.*
import java.util.function.Function
import java.util.function.Function.identity
import java.util.function.Supplier
import java.util.prefs.Preferences
import javax.enterprise.inject.spi.CDI

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
        private val preferencesStorage: PreferencesStorage = PlatformPreferencesStorage(Preferences.userRoot().node("name/wind/tools/process/browser"))

        val filterTextPreferencesEntry = PreferencesEntry(preferencesStorage, "filterText", FlatType(identity(), identity()), defaultBufferLifetime)
        val browseInitialDirectoryPreferencesEntry = PreferencesEntry<File>(preferencesStorage, "browseInitialDirectory", FlatType(Function { File(it) }, Function { it.absolutePath }), defaultBufferLifetime)
        val autoRefreshPreferencesEntry = PreferencesEntry<Boolean>(preferencesStorage, "autoRefresh", FlatType(Function { it?.toBoolean()?:false }, Function { it.toString() }), defaultBufferLifetime)

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
            fireEvent(StageFormLoad(ClassPathResource(FXML__PROCESS_LIST), Supplier { primaryStage }))
        }
    }

}