package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.application.fx.annotation.FXMLResource;
import com.github.windchopper.common.util.Pipeliner;
import javafx.geometry.Dimension2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.ResourceBundle;

@ApplicationScoped @FXMLResource(FXMLResources.FXML__NOT_WINDOWS) public class NotWindowsStageController
    extends AnyStageController {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

    @Override protected Dimension2D preferredStageSize() {
        return Pipeliner.of(Screen.getPrimary().getVisualBounds())
            .map(visualBounds -> new Dimension2D(visualBounds.getWidth() / 3, visualBounds.getHeight() / 3))
            .get();
    }

    @Override protected void start(Stage stage, String fxmlResource, Map<String, ?> parameters) {
        super.start(
            Pipeliner.of(() -> stage)
                .set(target -> target::setTitle, bundle.getString("stage.notWindows.title"))
                .get(),
            fxmlResource,
            parameters);
    }

}
