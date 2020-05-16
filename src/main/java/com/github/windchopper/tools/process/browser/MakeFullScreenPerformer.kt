package com.github.windchopper.tools.process.browser

import javafx.scene.control.Alert
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes

@ApplicationScoped class MakeFullScreenPerformer {

    class MakeFullScreen(val stageController: AnyStageController, val windowInfo: WindowInfo<*>)

    fun makeFullScreen(@Observes makeFullScreen: MakeFullScreen) {
        try {
            makeFullScreen.windowInfo.makeFullScreen()
        } catch (thrown: Exception) {
            makeFullScreen.stageController
                .prepareAlert(Alert.AlertType.ERROR, thrown.message)
                .show()
        }
    }

}