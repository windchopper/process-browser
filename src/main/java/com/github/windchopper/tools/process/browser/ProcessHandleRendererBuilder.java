package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.CellFactory;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;

import java.util.Optional;

import static java.util.Collections.singletonList;

public class ProcessHandleRendererBuilder {

    public static Callback<TreeTableColumn<ProcessHandleRepresentative, String>, TreeTableCell<ProcessHandleRepresentative, String>> treeTableCellFactory() {
        return CellFactory.treeTableColumnCellFactory(singletonList((cell, processHandle, value) -> {
            cell.setText(value);
            cell.setStyle(processHandle.parendPid() >= 0L
                ? "-fx-text-fill: #3b3b3b; -fx-font-style: italic"
                : "-fx-text-fill: #000000; -fx-font-style: normal");
        }));
    }

    public static Callback<TreeTableColumn.CellDataFeatures<ProcessHandleRepresentative, String>, ObservableValue<String>> treeTableCellValueFactory() {
        return CellFactory.treeTableColumnCellValueFactory(features -> {
            switch (features.getTreeTableColumn().getId()) {
                case "identifierColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue().getValue())
                            .filter(ProcessHandleRepresentative.class::isInstance)
                            .map(ProcessHandleRepresentative.class::cast)
                            .map(ProcessHandleRepresentative::pid)
                            .map(Object::toString)
                            .orElse(null));

                case "nameColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue().getValue())
                            .map(ProcessHandleRepresentative::name)
                            .orElse(null));

                case "executablePathColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue().getValue())
                            .map(ProcessHandleRepresentative::command)
                            .orElse(null));

                default:
                    return null;
            }
        });
    }

}
