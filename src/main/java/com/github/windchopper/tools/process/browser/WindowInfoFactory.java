package com.github.windchopper.tools.process.browser;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class WindowInfoFactory {

    public static boolean available() {
        return OperatingSystem.detect()
            .filter(OperatingSystem.WINDOWS::equals)
            .isPresent();
    }

    public static List<WindowInfo<?>> allWindowsOf(OperatingSystem operatingSystem, long pid) {
        return switch (operatingSystem) {
            case WINDOWS -> WindowsWindowInfo.allProcessWindows(pid);
            case MACOS, LINUX -> emptyList();
        };
    }

    public static List<WindowInfo<?>> allWindowsOf(long pid) {
        return OperatingSystem.detect()
            .map(system -> allWindowsOf(system, pid))
            .orElseGet(Collections::emptyList);
    }

}
