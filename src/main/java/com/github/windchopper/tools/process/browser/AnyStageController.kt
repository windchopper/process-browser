package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.cdi.form.StageFormController
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.stage.Modality
import javafx.stage.Stage

abstract class AnyStageController: StageFormController() {

    override fun afterLoad(form: Parent, parameters: Map<String, *>, formNamespace: Map<String, *>) {
        super.afterLoad(form, parameters, formNamespace)
        Image("/com/github/windchopper/tools/process/browser/images/Show-All-Views-50.png")
            .let { stage.icons.add(it) }
    }

    open fun prepareAlert(type: AlertType, message: String? = null, vararg buttonTypes: ButtonType): Alert {
        return Alert(type, null, *buttonTypes)
            .let { alert ->
                alert.initOwner(stage)
                alert.initModality(Modality.APPLICATION_MODAL)

                with (alert.dialogPane.scene.window) {
                    if (this is Stage) {
                        this.icons.addAll(stage.icons)
                    }
                }

                message?.let {
                    alert.headerText = message
                }

                alert
            }
    }

}