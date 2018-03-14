package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.CellFactory;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.util.Optional;

import static java.util.Collections.singletonList;

public class ProcessHandleRendererBuilder {

    public static Callback<TableColumn<ProcessHandleRepresentative, String>, TableCell<ProcessHandleRepresentative, String>> tableCellFactory() {
        return CellFactory.tableColumnCellFactory(singletonList((cell, processHandle, value) -> cell.setText(value)));
    }

    public static Callback<TableColumn.CellDataFeatures<ProcessHandleRepresentative, String>, ObservableValue<String>> tableCellValueFactory() {
        return CellFactory.tableColumnCellValueFactory(features -> {
            switch (features.getTableColumn().getId()) {
                case "identifierColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue())
                            .map(ProcessHandleRepresentative::pid)
                            .map(Object::toString)
                            .orElse(null));

                case "parentIdentifierColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue())
                            .map(ProcessHandleRepresentative::parendPid)
                            .filter(pid -> pid >= 0)
                            .map(Object::toString)
                            .orElse(null));

                case "nameColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue())
                            .map(ProcessHandleRepresentative::name)
                            .orElse(null));

                case "executablePathColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue())
                            .map(ProcessHandleRepresentative::command)
                            .orElse(null));

                default:
                    return null;
            }
        });
    }

}
