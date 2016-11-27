package name.wind.tools.process.browser;

import name.wind.common.preferences.PreferencesEntry;
import name.wind.common.preferences.types.FlatType;

import java.io.File;
import java.time.Duration;
import java.util.prefs.Preferences;

public interface PreferencesAware {

    Preferences preferencesNode = Preferences.userRoot().node("/name/wind/tools/process/browser");
    Duration defaultBufferLifetime = Duration.ofMinutes(1);

    String PREFERENCES_ENTRY_NAME__FILTER_TEXT = "filterText";
    String PREFERENCES_ENTRY_NAME__BROWSE_INITIAL_DIRECTORY = "browseInitialDirectory";

    PreferencesEntry<String> filterTextPreferencesEntry = new PreferencesEntry<>(
        preferencesNode, PREFERENCES_ENTRY_NAME__FILTER_TEXT, new FlatType<>(string -> string, string -> string), defaultBufferLifetime);

    PreferencesEntry<File> browseInitialDirectoryPreferencesEntry = new PreferencesEntry<>(
        preferencesNode, PREFERENCES_ENTRY_NAME__BROWSE_INITIAL_DIRECTORY, new FlatType<>(File::new, File::getAbsolutePath), defaultBufferLifetime);

}
