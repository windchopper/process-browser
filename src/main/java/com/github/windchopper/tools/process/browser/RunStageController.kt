package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.cdi.form.Form
import jakarta.enterprise.context.ApplicationScoped
import javafx.beans.binding.Bindings
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import java.io.File

@ApplicationScoped @Form(Application.FXML__RUN) class RunStageController: AnyStageController() {

    @FXML private lateinit var commandTextField: TextField
    @FXML private lateinit var elevateCheckBox: CheckBox
    @FXML private lateinit var okButton: Button

    override fun afterLoad(form: Parent, parameters: Map<String, *>, formNamespace: Map<String, *>) {
        super.afterLoad(form, parameters, formNamespace)
        stage.title = Application.messages["stage.run.title"]
        okButton.disableProperty().bind(Bindings.isEmpty(commandTextField.textProperty()))
        elevateCheckBox.disableProperty().bind(Bindings.createBooleanBinding({ false }).not())
    }

    @FXML fun browse(event: ActionEvent) {
        val chooser = FileChooser()
            .let { fileChooser ->
                fileChooser.initialDirectory = Application.browseInitialDirectoryPreferencesEntry.load().value
                    ?:System.getProperty("user.home")?.let(::File)
                fileChooser
            }

        chooser.showOpenDialog(stage)
            ?.let {
                Application.browseInitialDirectoryPreferencesEntry.save(it.parentFile)
                commandTextField.text = it.absolutePath
            }
    }

    @FXML fun run(event: ActionEvent) {
        exceptionally {
            ProcessBuilder(commandTextField.text)
                .start()
        }
    }

}