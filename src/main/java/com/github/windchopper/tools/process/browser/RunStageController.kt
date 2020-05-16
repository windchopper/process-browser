package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.cdi.form.Form
import javafx.beans.binding.Bindings
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import java.io.File
import java.util.concurrent.Callable
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped @Form(Application.FXML__RUN) class RunStageController: AnyStageController() {

    @FXML protected lateinit var commandTextField: TextField
    @FXML protected lateinit var elevateCheckBox: CheckBox
    @FXML protected lateinit var okButton: Button

    override fun afterLoad(form: Parent, parameters: Map<String, *>, formNamespace: Map<String, *>) {
        super.afterLoad(form, parameters, formNamespace)
        stage.title = Application.messages.getString("stage.run.title")
        okButton.disableProperty().bind(Bindings.isEmpty(commandTextField.textProperty()))
        elevateCheckBox.disableProperty().bind(Bindings.createBooleanBinding(Callable { false }).not())
    }

    @FXML protected fun browse(event: ActionEvent) {
        val chooser = FileChooser()
            .let {
                it.initialDirectory = Application.browseInitialDirectoryPreferencesEntry.load()
                    ?: System.getProperty("user.home")?.let { File(it) }
                it
            }

        chooser.showOpenDialog(stage)
            ?.let {
                Application.browseInitialDirectoryPreferencesEntry.save(it.parentFile)
                commandTextField.text = it.absolutePath
            }
    }

    @FXML protected fun run(event: ActionEvent) {
        try {
            ProcessBuilder(commandTextField.text).start()
        } catch (thrown: Exception) {
            thrown.display(this)
        }
    }

}