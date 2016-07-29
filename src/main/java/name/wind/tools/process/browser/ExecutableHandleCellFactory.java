package name.wind.tools.process.browser;

import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.util.Callback;
import name.wind.tools.process.browser.windows.ExecutableHandle;
import name.wind.tools.process.browser.windows.ProcessModuleHandle;

public class ExecutableHandleCellFactory implements Callback<TreeTableColumn<ExecutableHandle, String>, TreeTableCell<ExecutableHandle, String>> {

    public ExecutableHandleCellFactory() {
    }

    @Override public TreeTableCell<ExecutableHandle, String> call(TreeTableColumn<ExecutableHandle, String> column) {
        return new TreeTableCell<ExecutableHandle, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                TreeTableRow<ExecutableHandle> row = getTreeTableRow();
                setStyle(row.getTreeItem() != null && row.getTreeItem().getValue() instanceof ProcessModuleHandle
                    ? "-fx-text-fill: #3b3b3b; -fx-font-style: italic"
                    : "-fx-text-fill: #000000; -fx-font-style: normal");
            }
        };
    }

}
