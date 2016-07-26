package name.wind.tools.process.browser;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import name.wind.tools.process.browser.windows.ProcessHandle;
import name.wind.tools.process.browser.windows.jna.Kernel32Extended;
import name.wind.tools.process.browser.windows.jna.Shell32Extended;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface WindowsRoutines {

    default String windowTitle(WinDef.HWND hwnd) {
        int bufferLength = User32.INSTANCE.GetWindowTextLength(hwnd);

        if (bufferLength > 0) {
            char[] buffer = new char[bufferLength + 1];

            if (0 == User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length)) {
                throw new Win32Exception(
                    Kernel32Extended.INSTANCE.GetLastError());
            }

            return new String(buffer);
        }

        return null;
    }

    default List<WinDef.HWND> processWindows(ProcessHandle processHandle) {
        List<WinDef.HWND> windowHandles = new ArrayList<>();

        if (User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
            if (User32.INSTANCE.IsWindowVisible(hwnd)) {
                IntByReference windowProcess = new IntByReference();

                if (0 == User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowProcess)) {
                    throw new Win32Exception(
                        Kernel32Extended.INSTANCE.GetLastError());
                }

                if (windowProcess.getValue() == processHandle.identifier()) {
                    System.out.printf("hwnd: %s, title: %s\n", hwnd, windowTitle(hwnd));
                    windowHandles.add(hwnd);
                }
            }

            return true;
        }, null)) {
            return windowHandles;
        } else {
            throw new Win32Exception(
                Kernel32Extended.INSTANCE.GetLastError());
        }
    }

//    public void RemoveFrame()
//    {
//        // get the current window style
//        var originalWindowStyle = SafeNativeMethods.GetWindowLong(hwnd, WindowSettings.GWL_STYLE);
//
//        if (originalWindowStyle == 0)
//        {
//            throw new Win32Exception(Marshal.GetLastWin32Error(), string.Format("Failed to get the window style for hwnd {0}.", hwnd));
//        }
//
//        // remove the caption, frame, minimize button, maximize button, and close button from the window style
//        var modifiedWindowStyle = originalWindowStyle & ~(WindowStyleFlags.WS_CAPTION | WindowStyleFlags.WS_THICKFRAME | WindowStyleFlags.WS_MINIMIZE | WindowStyleFlags.WS_MAXIMIZE | WindowStyleFlags.WS_SYSMENU);
//
//        // if there is a difference in the window style, then proceed to modify the style
//        if (modifiedWindowStyle != originalWindowStyle)
//        {
//            // apply the adjusted window style to the window
//            var previousWindowStyle = SafeNativeMethods.SetWindowLong(hwnd, WindowSettings.GWL_STYLE, modifiedWindowStyle);
//
//            if (previousWindowStyle == 0)
//            {
//                throw new Win32Exception(Marshal.GetLastWin32Error(), string.Format("Failed to set the window style for hwnd {0}.", hwnd));
//            }
//        }
//    }

//    public void SetPositionAndSize(int left, int top, int width, int height)
//    {
//        if (width < 0)
//        {
//            throw new ArgumentOutOfRangeException("width", "The width cannot be negative.");
//        }
//
//        if (height < 0)
//        {
//            throw new ArgumentOutOfRangeException("height", "The height cannot be negative.");
//        }
//
//        //int resultOfSetWindowPos = SafeNativeMethods.SetWindowPos(hwnd, IntPtr.Zero, -borderThickness, -titleBarHeight, screenWidth + (2 * borderThickness), screenHeight + (titleBarHeight + borderThickness), SetWindowPosFlags.SWP_FRAMECHANGED);
//        int resultOfSetWindowPos = SafeNativeMethods.SetWindowPos(hwnd, IntPtr.Zero, left, top, width, height, (uint)SetWindowPosFlags.SWP_FRAMECHANGED);
//
//        if (resultOfSetWindowPos == 0)
//        {
//            throw new Win32Exception(Marshal.GetLastWin32Error(), string.Format("Failed to set the position and size of hwnd {0}.", hwnd));
//        }
//    }

}
