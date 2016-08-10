package name.wind.tools.process.browser.windows;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import name.wind.common.fx.CellFactory;
import name.wind.common.util.Value;

import java.nio.file.Path;

import static java.util.Collections.singletonList;

public interface ExecutableHandle {

    Path path();

    default String name() {
        return path().getFileName().toString();
    }

    static Callback<TreeTableColumn<ExecutableHandle, String>, TreeTableCell<ExecutableHandle, String>> treeTableCellFactory() {
        return CellFactory.treeTableColumnCellFactory(singletonList((cell, executableHandle, value) -> {
            cell.setText(value);
            cell.setStyle(executableHandle instanceof ProcessModuleHandle
                ? "-fx-text-fill: #3b3b3b; -fx-font-style: italic"
                : "-fx-text-fill: #000000; -fx-font-style: normal");
        }));
    }

    static Callback<TreeTableColumn.CellDataFeatures<ExecutableHandle, String>, ObservableValue<String>> treeTableCellValueFactory() {
        return CellFactory.treeTableColumnCellValueFactory(features -> {
            switch (features.getTreeTableColumn().getId()) {
                case "identifierColumn":
                    return new ReadOnlyStringWrapper(
                        Value.of(features.getValue().getValue())
                            .filter(ProcessHandle.class::isInstance)
                            .map(ProcessHandle.class::cast)
                            .map(ProcessHandle::identifier)
                            .map(Object::toString)
                            .orElse(null));

                case "nameColumn":
                    return new ReadOnlyStringWrapper(
                        Value.of(features.getValue().getValue())
                            .map(ExecutableHandle::name)
                            .orElse(null));

                case "executablePathColumn":
                    return new ReadOnlyStringWrapper(
                        Value.of(features.getValue().getValue())
                            .map(ExecutableHandle::path)
                            .map(Object::toString)
                            .orElse(null));

                default:
                    return null;
            }
        });
    }

}
