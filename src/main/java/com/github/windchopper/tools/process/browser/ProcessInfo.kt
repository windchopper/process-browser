package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.CellFactories;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ProcessInfo {

    private final long pid;
    private final long parentPid;
    private final String name;
    private final String command;

    private final StringProperty pidProperty;
    private final StringProperty parentPidProperty;
    private final StringProperty nameProperty;
    private final StringProperty commandProperty;

    public ProcessInfo(ProcessHandle processHandle) {
        pid = processHandle.pid();
        parentPid = processHandle.parent()
            .map(ProcessHandle::pid)
            .orElse(-1L);
        name = processHandle.info().command()
            .map(Paths::get)
            .map(Path::getFileName)
            .map(Objects::toString)
            .orElse("?");
        command = processHandle.info().command()
            .orElse("?");

        pidProperty = new ReadOnlyStringWrapper(Objects.toString(pid));
        parentPidProperty = new ReadOnlyStringWrapper(parentPid < 0 ? "" : Objects.toString(parentPid));
        nameProperty = new ReadOnlyStringWrapper(name);
        commandProperty = new ReadOnlyStringWrapper(command);
    }

    public long pid() {
        return pid;
    }

    public long parentPid() {
        return parentPid;
    }

    public String name() {
        return name;
    }

    public String command() {
        return command;
    }

    public void destroyForcibly() {
        ProcessHandle.of(pid)
            .ifPresent(ProcessHandle::destroyForcibly);
    }

    public static Callback<TableColumn<ProcessInfo, String>, TableCell<ProcessInfo, String>> tableCellFactory() {
        return CellFactories.tableColumnCellFactory((cell, column, item, empty) -> cell.setText(item));
    }

    public static Callback<TableColumn.CellDataFeatures<ProcessInfo, String>, ObservableValue<String>> tableCellValueFactory() {
        return (features) -> features.getValue() == null ? null : switch (features.getTableColumn().getId()) {
            default -> null;
            case "identifierColumn" -> features.getValue().pidProperty;
            case "parentIdentifierColumn" -> features.getValue().parentPidProperty;
            case "nameColumn" -> features.getValue().nameProperty;
            case "executablePathColumn" -> features.getValue().commandProperty;
        };
    }

}
