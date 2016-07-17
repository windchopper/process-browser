package name.wind.tools.process.browser;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import name.wind.common.util.Builder;
import name.wind.tools.process.browser.events.StageConstructed;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;

@ApplicationScoped public class ProcessListStageController extends AbstractStageController {

    private Parent buildSceneRoot() {
        return Builder.direct(GridPane::new)
            .get();
    }

    private void start(@Observes @Named(StageConstructed.IDENTIFIER__PROCESS_LIST) StageConstructed stageConstructed) {
        super.start(
            stageConstructed.stage(),
            stageConstructed.identifier(),
            stageConstructed.preferredSize());

        Builder.direct(() -> stage)
            .set(target -> target::setScene, Builder.direct(() -> new Scene(buildSceneRoot()))
                //.add(target -> target::getStylesheets, singletonList("/name/wind/tools/ldap/browser/connectionStage.css"))
                .get())
            .get().show();
    }

}
