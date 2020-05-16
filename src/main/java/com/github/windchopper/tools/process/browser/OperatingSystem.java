package com.github.windchopper.tools.process.browser;

import java.util.EnumSet;
import java.util.Optional;

public enum OperatingSystem {

    WINDOWS("win"),
    MACOS("mac"),
    LINUX("nux");

    private final String token;

    OperatingSystem(String token) {
        this.token = token;
    }

    public static Optional<OperatingSystem> detect() {
        Optional<String> operatingSystemNameProperty = Optional.of("os.name")
            .map(System::getProperty)
            .map(String::toLowerCase);

        return EnumSet.allOf(OperatingSystem.class).stream()
            .filter(system -> operatingSystemNameProperty
                .filter(name -> name.contains(system.token))
                .isPresent())
            .findFirst();
    }

}
