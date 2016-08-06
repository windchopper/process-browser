package name.wind.tools.process.browser.windows;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Win32Exception;
import name.wind.tools.process.browser.windows.jna.Kernel32Extended;
import name.wind.tools.process.browser.windows.jna.PsapiExtended;
import name.wind.tools.process.browser.windows.jna.Shell32Extended;

public interface JnaAware {

    Kernel32Extended kernel = Kernel32Extended.INSTANCE;
    PsapiExtended psapi = PsapiExtended.INSTANCE;
    Advapi32 advapi = Advapi32.INSTANCE;
    Shell32Extended shell = Shell32Extended.INSTANCE;
    User32 user = User32.INSTANCE;

    class LastWin32Exception extends Win32Exception {

        public LastWin32Exception() {
            super(kernel.GetLastError());
        }

    }

}
