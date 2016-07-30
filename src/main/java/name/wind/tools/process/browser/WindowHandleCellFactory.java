package name.wind.tools.process.browser;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import name.wind.tools.process.browser.windows.WindowHandle;

public class WindowHandleCellFactory implements Callback<ListView<WindowHandle>, ListCell<WindowHandle>>, ResourceBundleAware {

    @Override public ListCell<WindowHandle> call(ListView<WindowHandle> listView) {
        return new ListCell<WindowHandle>() {
            @Override protected void updateItem(WindowHandle item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null) {
                    setText(null);
                } else if (item.title() == null) {
                    setText(bundle.getString("stage.selection.emptyTitle"));
                } else {
                    setText(item.title());
                }
            }
        };
    }

}
