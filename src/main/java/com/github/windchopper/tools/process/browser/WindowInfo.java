package com.github.windchopper.tools.process.browser;

import java.util.ResourceBundle;

public abstract class WindowInfo<NativeHandleType> {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

    protected final NativeHandleType nativeHandle;
    protected final String title;

    public WindowInfo(NativeHandleType nativeHandle, String title) {
        this.nativeHandle = nativeHandle;
        this.title = title;
    }

    public abstract void makeFullScreen();

    @Override public String toString() {
        return String.format("%s: %s", title != null ? title : bundle.getString("stage.selection.emptyTitle"), nativeHandle);
    }

}
