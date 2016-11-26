package name.wind.tools.process.browser;

import name.wind.common.preferences.DefaultPreferencesEntry;
import name.wind.common.preferences.PreferencesEntry;
import name.wind.common.preferences.PreferencesEntryType;
import name.wind.common.preferences.store.DefaultFlatPreferencesStorage;
import name.wind.common.preferences.store.PreferencesStorage;

import java.io.File;
import java.time.Duration;

public interface PreferencesAware {

    PreferencesStorage<String> preferencesStorage = new DefaultFlatPreferencesStorage("/name/wind/tools/process/browser");

    String PREFERENCES_ENTRY_NAME__FILTER_TEXT = "filterText";
    String PREFERENCES_ENTRY_NAME__BROWSE_INITIAL_DIRECTORY = "browseInitialDirectory";

    PreferencesEntry<String> filterTextPreferencesEntry = new DefaultPreferencesEntry<>(
        Duration.ofMinutes(1),
        preferencesStorage,
        PREFERENCES_ENTRY_NAME__FILTER_TEXT,
        PreferencesEntryType.stringType);

    PreferencesEntry<File> browseInitialDirectoryPreferencesEntry = new DefaultPreferencesEntry<>(
        Duration.ofMinutes(1),
        preferencesStorage,
        PREFERENCES_ENTRY_NAME__BROWSE_INITIAL_DIRECTORY,
        PreferencesEntryType.fileType);

}
