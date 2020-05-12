package com.github.windchopper.tools.process.browser;

import java.util.List;

import static java.util.Collections.emptyList;

public class WindowInfoFactory {

    public static boolean available() {
        return OperatingSystem.detect() == OperatingSystem.WINDOWS;
    }

    public static List<WindowInfo<?>> allWindowsOf(long pid) {
        return switch (OperatingSystem.detect()) {
            case WINDOWS -> WindowsWindowInfo.allProcessWindows(pid);
            case MACOS, LINUX, UNKNOWN -> emptyList();
        };
    }

}
