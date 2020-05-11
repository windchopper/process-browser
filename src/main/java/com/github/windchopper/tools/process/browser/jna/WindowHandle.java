package com.github.windchopper.tools.process.browser.jna;

import com.github.windchopper.common.fx.CellFactories;
import com.sun.jna.platform.win32.WinDef;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

import java.util.Optional;
import java.util.ResourceBundle;

public class WindowHandle {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("com.github.windchopper.tools.process.browser.i18n.messages");

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

    public static Callback<ListView<WindowHandle>, ListCell<WindowHandle>> listCellFactory() {
        return CellFactories.listCellFactory((cell, item, empty) -> cell.setText(Optional.ofNullable(item)
            .map(WindowHandle::title)
            .orElse(bundle.getString("stage.selection.emptyTitle"))));
    }

}
