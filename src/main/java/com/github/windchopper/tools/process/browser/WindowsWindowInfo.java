package com.github.windchopper.tools.process.browser;

import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

public class WindowsWindowInfo extends WindowInfo<WinDef.HWND> {

    private static final User32 user = User32.INSTANCE;
    private static final Kernel32 kernel = Kernel32.INSTANCE;

    private static final Logger logger = Logger.getLogger(WindowInfo.class.getName());

    public WindowsWindowInfo(WinDef.HWND nativeHandle, String title) {
        super(nativeHandle, title);
    }

    @Override public void makeFullScreen() {
        removeWindowFrame(nativeHandle);
        applyMonitorSizeToWindow(nativeHandle);
    }

    private static void throwLastError() {
        checkLastError(errorCode -> {
            throw new Win32Exception(errorCode);
        });
    }

    private static void checkLastError(IntConsumer handler) {
        int errorCode = kernel.GetLastError();

        if (errorCode > 0) {
            handler.accept(errorCode);
        }
    }

    private static String windowTitle(WinDef.HWND nativeHandle) {
        var textLength = user.GetWindowTextLength(nativeHandle);

        if (textLength > 0) {
            char[] buffer = new char[textLength + 1];

            if (user.GetWindowText(nativeHandle, buffer, buffer.length) > 0) {
                return new String(buffer);
            }
        }

        throwLastError();

        return null;
    }

    private void applyMonitorSizeToWindow(WinDef.HWND windowHandle) {
        var monitorHandle = user.MonitorFromWindow(windowHandle, User32.MONITOR_DEFAULTTONEAREST);

        throwLastError();

        var monitorInfo = new WinUser.MONITORINFOEX();

        if (user.GetMonitorInfo(monitorHandle, monitorInfo).booleanValue()) {
            user.SetWindowPos(windowHandle, null, monitorInfo.rcMonitor.left, monitorInfo.rcMonitor.top, monitorInfo.rcMonitor.right, monitorInfo.rcMonitor.bottom, User32.SWP_FRAMECHANGED);
        }

        throwLastError();
    }

    private void removeWindowFrame(WinDef.HWND windowHandle) {
        var originalWindowStyle = user.GetWindowLong(windowHandle, User32.GWL_STYLE);

        throwLastError();

        var modifiedWindowStyle = originalWindowStyle & ~(User32.WS_CAPTION | User32.WS_THICKFRAME | User32.WS_MINIMIZE | User32.WS_MAXIMIZE | User32.WS_SYSMENU);

        if (modifiedWindowStyle != originalWindowStyle) {
            user.SetWindowLong(windowHandle, User32.GWL_STYLE, modifiedWindowStyle);
            throwLastError();
        }
    }

    public static List<WindowInfo<?>> allProcessWindows(long pid) {
        var windowInfoList = new ArrayList<WindowInfo<?>>();

        WinUser.WNDENUMPROC windowEnumerator = (handle, pointer) -> {
            if (user.IsWindowVisible(handle)) {
                IntByReference windowProcess = new IntByReference();

                user.GetWindowThreadProcessId(handle, windowProcess);

                checkLastError(code -> logger.severe(Kernel32Util.formatMessage(code)));

                if (windowProcess.getValue() == (int) pid) {
                    windowInfoList.add(new WindowsWindowInfo(handle, windowTitle(handle)));
                }
            }

            return true;
        };

        user.EnumWindows(windowEnumerator, null);

        throwLastError();

        return windowInfoList;
    }

}
