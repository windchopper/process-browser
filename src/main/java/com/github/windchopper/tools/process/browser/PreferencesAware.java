package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.preferences.PlatformPreferencesStorage;
import com.github.windchopper.common.preferences.PreferencesEntry;
import com.github.windchopper.common.preferences.PreferencesStorage;
import com.github.windchopper.common.preferences.types.FlatType;

import java.io.File;
import java.time.Duration;
import java.util.prefs.Preferences;

public interface PreferencesAware {

    Duration defaultBufferLifetime = Duration.ofMinutes(1);
    PreferencesStorage preferencesStorage = new PlatformPreferencesStorage(
        Preferences.userRoot().node("name/wind/tools/process/browser"));

    String PREFERENCES_ENTRY_NAME__FILTER_TEXT = "filterText";
    String PREFERENCES_ENTRY_NAME__BROWSE_INITIAL_DIRECTORY = "browseInitialDirectory";
    String PREFERENCES_ENTRY_NAME__AUTO_REFRESH = "autoRefresh";

    PreferencesEntry<String> filterTextPreferencesEntry = new PreferencesEntry<>(
        preferencesStorage, PREFERENCES_ENTRY_NAME__FILTER_TEXT, new FlatType<>(string -> string, string -> string), defaultBufferLifetime);

    PreferencesEntry<File> browseInitialDirectoryPreferencesEntry = new PreferencesEntry<>(
        preferencesStorage, PREFERENCES_ENTRY_NAME__BROWSE_INITIAL_DIRECTORY, new FlatType<>(File::new, File::getAbsolutePath), defaultBufferLifetime);

    PreferencesEntry<Boolean> autoRefreshPreferencesEntry = new PreferencesEntry<>(
        preferencesStorage, PREFERENCES_ENTRY_NAME__AUTO_REFRESH, new FlatType<>(Boolean::parseBoolean, Object::toString), defaultBufferLifetime);

}
