package com.github.windchopper.tools.process.browser;

import java.util.Optional;

public enum OperatingSystem {

    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN;

    public static OperatingSystem detect() {
        Optional<String> operatingSystemNameProperty = Optional.of("os.name")
            .map(System::getProperty)
            .map(String::toLowerCase);

        if (operatingSystemNameProperty
                .filter(name -> name.contains("win"))
                .isPresent()) {
            return WINDOWS;
        }

        if (operatingSystemNameProperty
                .filter(name -> name.contains("nux"))
                .isPresent()) {
            return LINUX;
        }

        if (operatingSystemNameProperty
                .filter(name -> name.contains("mac"))
                .isPresent()) {
            return MACOS;
        }

        return UNKNOWN;
    }

}
