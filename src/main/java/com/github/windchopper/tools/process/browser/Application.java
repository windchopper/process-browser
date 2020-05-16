package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.cdi.ResourceBundleLoad;
import com.github.windchopper.common.fx.cdi.form.StageFormLoad;
import com.github.windchopper.common.preferences.PlatformPreferencesStorage;
import com.github.windchopper.common.preferences.PreferencesEntry;
import com.github.windchopper.common.preferences.PreferencesStorage;
import com.github.windchopper.common.preferences.types.FlatType;
import com.github.windchopper.common.util.ClassPathResource;
import javafx.stage.Stage;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import java.io.File;
import java.time.Duration;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static java.util.function.Function.identity;

public class Application extends javafx.application.Application {

    public static final String FXML__PROCESS_LIST = "com/github/windchopper/tools/process/browser/processListStage.fxml";
    public static final String FXML__SELECTION = "com/github/windchopper/tools/process/browser/selectionStage.fxml";
    public static final String FXML__RUN = "com/github/windchopper/tools/process/browser/runStage.fxml";

    public static final ResourceBundle messages = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

    public static final Duration defaultBufferLifetime = Duration.ofMinutes(1);

    public static final PreferencesStorage preferencesStorage = new PlatformPreferencesStorage(
        Preferences.userRoot().node("name/wind/tools/process/browser"));

    public static final PreferencesEntry<String> filterTextPreferencesEntry = new PreferencesEntry<>(
        new PlatformPreferencesStorage(
            Preferences.userRoot().node("name/wind/tools/process/browser")), "filterText", new FlatType<>(identity(), identity()), Duration.ofMinutes(1));

    public static final PreferencesEntry<File> browseInitialDirectoryPreferencesEntry = new PreferencesEntry<>(
        new PlatformPreferencesStorage(
            Preferences.userRoot().node("name/wind/tools/process/browser")), "browseInitialDirectory", new FlatType<>(File::new, File::getAbsolutePath), Duration.ofMinutes(1));

    public static final PreferencesEntry<Boolean> autoRefreshPreferencesEntry = new PreferencesEntry<Boolean>(
        new PlatformPreferencesStorage(
            Preferences.userRoot().node("name/wind/tools/process/browser")), "autoRefresh", new FlatType<>(Boolean::valueOf, Object::toString), Duration.ofMinutes(1));

    private Weld weld;
    private WeldContainer weldContainer;

    @Override public void init() {
        weld = new Weld();
        weldContainer = weld.initialize();
    }

    @Override public void stop() {
        weld.shutdown();
    }

    @Override public void start(Stage primaryStage) {
        var beanManager = weldContainer.getBeanManager();

        beanManager.fireEvent(
            new ResourceBundleLoad(messages));
        beanManager.fireEvent(
            new StageFormLoad(new ClassPathResource(FXML__PROCESS_LIST), () -> primaryStage));
    }

    public static void main(String... args) {
        launch(args);
    }

}
