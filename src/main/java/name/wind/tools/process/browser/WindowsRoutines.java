package name.wind.tools.process.browser;

import com.sun.jna.platform.win32.*;

import java.util.ArrayList;
import java.util.List;

public class WindowsRoutines {

    public static List<WinDef.HWND> listProcessWindowHandles(ProcessHandle processHandle) {
        List<WinDef.HWND> windowHandles = new ArrayList<>();

        WinNT.HANDLE nativeProcessHandle = Kernel32.INSTANCE.OpenProcess(
            Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ, false, (int) processHandle.getPid());

        try {
            if (User32.INSTANCE.EnumWindows((hwnd, pointer) -> windowHandles.add(hwnd), null)) {
                return windowHandles;
            } else {
                throw new Win32Exception(
                    Kernel32.INSTANCE.GetLastError());
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(nativeProcessHandle);
        }
    }

}
