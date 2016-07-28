package name.wind.tools.process.browser.windows;

import com.sun.jna.platform.win32.WinDef;

public class WindowHandle {

    private final WinDef.HWND handle;
    private final String title;

    public WindowHandle(WinDef.HWND handle, String title) {
        this.handle = handle;
        this.title = title;
    }

    public WinDef.HWND handle() {
        return handle;
    }

    public String title() {
        return title;
    }

}
