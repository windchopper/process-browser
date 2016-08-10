package name.wind.tools.process.browser.windows;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import name.wind.tools.process.browser.windows.jna.User32Extended;

import java.util.ArrayList;
import java.util.List;

public class WindowRoutines implements JnaAware {

    public static String windowTitle(WinDef.HWND hwnd) {
        int bufferLength = user.GetWindowTextLength(hwnd);

        if (bufferLength > 0) {
            char[] buffer = new char[bufferLength + 1];

            if (0 == user.GetWindowText(hwnd, buffer, buffer.length)) {
                throw new Win32Exception(kernel.GetLastError());
            }

            return new String(buffer);
        }

        return null;
    }

    public static List<WindowHandle> processWindowHandles(ProcessHandle processHandle) {
        List<WindowHandle> windowHandles = new ArrayList<>();

        if (user.EnumWindows((hwnd, pointer) -> {
            if (user.IsWindowVisible(hwnd)) {
                IntByReference windowProcess = new IntByReference();

                if (0 == user.GetWindowThreadProcessId(hwnd, windowProcess)) {
                    throw new Win32Exception(kernel.GetLastError());
                }

                if (windowProcess.getValue() == processHandle.identifier()) {
                    windowHandles.add(
                        new WindowHandle(
                            hwnd, windowTitle(hwnd)));
                }
            }

            return true;
        }, null)) {
            return windowHandles;
        } else {
            throw new Win32Exception(kernel.GetLastError());
        }
    }

    public static void applyMonitorSizeToWindow(WindowHandle windowHandle) {
        WinUser.HMONITOR hMonitor = user.MonitorFromWindow(windowHandle.handle(), User32.MONITOR_DEFAULTTONEAREST);
        if (hMonitor == null) {
            throw new Win32Exception(kernel.GetLastError());
        }

        User32.MONITORINFOEX monitorInfo = new WinUser.MONITORINFOEX();
        if (user.GetMonitorInfo(hMonitor, monitorInfo).booleanValue()) {
            if (!user.SetWindowPos(
                    windowHandle.handle(),
                    null,
                    monitorInfo.rcMonitor.left,
                    monitorInfo.rcMonitor.top,
                    monitorInfo.rcMonitor.right,
                    monitorInfo.rcMonitor.bottom,
                    User32Extended.SWP_FRAMECHANGED)) {
                throw new Win32Exception(kernel.GetLastError());
            }
        } else {
            throw new Win32Exception(kernel.GetLastError());
        }
    }

    public static void removeWindowFrame(WindowHandle windowHandle) {
        int originalWindowStyle = user.GetWindowLong(windowHandle.handle(), User32.GWL_STYLE);

        if (originalWindowStyle == 0) {
            throw new Win32Exception(kernel.GetLastError());
        }

        int modifiedWindowStyle = originalWindowStyle & ~(User32.WS_CAPTION | User32.WS_THICKFRAME | User32.WS_MINIMIZE | User32.WS_MAXIMIZE | User32.WS_SYSMENU);

        if (modifiedWindowStyle != originalWindowStyle) {
            int previousWindowStyle = user.SetWindowLong(windowHandle.handle(), User32.GWL_STYLE, modifiedWindowStyle);

            if (previousWindowStyle == 0) {
                throw new Win32Exception(kernel.GetLastError());
            }
        }
    }

}
