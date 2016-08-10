package name.wind.tools.process.browser.windows;

import com.sun.jna.platform.win32.WinDef;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import name.wind.common.fx.CellFactory;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.ResourceBundleAware;

import static java.util.Collections.singletonList;

public class WindowHandle implements ResourceBundleAware {

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

    @SuppressWarnings("unchecked") public static Callback<ListView<WindowHandle>, ListCell<WindowHandle>> listCellFactory() {
        return CellFactory.listCellFactory(singletonList((cell, item) -> cell.setText(Value.of(item)
            .map(WindowHandle::title).orElse(bundle.getString("stage.selection.emptyTitle")))));
    }

}
