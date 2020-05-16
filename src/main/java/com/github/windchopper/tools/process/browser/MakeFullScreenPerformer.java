package com.github.windchopper.tools.process.browser;

import javafx.scene.control.Alert;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped public class MakeFullScreenPerformer {

    public static class MakeFullScreen {

        final AnyStageController stageController;
        final WindowInfo<?> windowInfo;

        public MakeFullScreen(AnyStageController stageController, WindowInfo<?> windowInfo) {
            this.stageController = stageController;
            this.windowInfo = windowInfo;
        }

    }

    public void makeFullScreen(@Observes MakeFullScreen makeFullScreen) {
        try {
            makeFullScreen.windowInfo.makeFullScreen();
        } catch (Exception thrown) {
            makeFullScreen.stageController.prepareAlert(Alert.AlertType.ERROR)
                .set(bean -> bean::setHeaderText, thrown.getMessage())
                .get().show();
        }
    }

}
