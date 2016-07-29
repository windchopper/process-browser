package name.wind.tools.process.browser;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import name.wind.common.util.Value;
import name.wind.tools.process.browser.windows.ExecutableHandle;
import name.wind.tools.process.browser.windows.ProcessHandle;

public class ExecutableHandleCellValueFactory implements Callback<TreeTableColumn.CellDataFeatures<ExecutableHandle, String>, ObservableValue<String>> {

    @Override public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<ExecutableHandle, String> features) {
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
    }

}
