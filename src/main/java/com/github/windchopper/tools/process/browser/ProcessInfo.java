package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.CellFactories;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ProcessInfo {

    private final long pid;
    private final long parentPid;
    private final String name;
    private final String command;

    public ProcessInfo(ProcessHandle processHandle) {
        pid = processHandle.pid();
        parentPid = processHandle.parent()
            .map(ProcessHandle::pid)
            .orElse(-1L);
        name = processHandle.info().command()
            .map(Paths::get)
            .map(Path::getFileName)
            .map(Object::toString)
            .orElse("?");
        command = processHandle.info().command()
            .orElse("?");
    }

    public long pid() {
        return pid;
    }

    public long parendPid() {
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
        return (features) -> switch (features.getTableColumn().getId()) {
            default -> null;
            case "identifierColumn" -> new ReadOnlyStringWrapper(Optional.ofNullable(features.getValue())
                .map(ProcessInfo::pid)
                .map(Object::toString)
                .orElse(null));
            case "parentIdentifierColumn" -> new ReadOnlyStringWrapper(Optional.ofNullable(features.getValue())
                .map(ProcessInfo::parendPid)
                .filter(pid -> pid >= 0)
                .map(Object::toString)
                .orElse(null));
            case "nameColumn" -> new ReadOnlyStringWrapper(Optional.ofNullable(features.getValue())
                .map(ProcessInfo::name)
                .orElse(null));
            case "executablePathColumn" -> new ReadOnlyStringWrapper(Optional.ofNullable(features.getValue())
                .map(ProcessInfo::command)
                .orElse(null));
        };
    }

}
