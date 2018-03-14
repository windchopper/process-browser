package com.github.windchopper.tools.process.browser;

import com.github.windchopper.common.fx.CellFactory;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class ProcessInfo {

    private final long pid;
    private final long parendPid;
    private final String name;
    private final String command;

    public ProcessInfo(ProcessHandle processHandle) {
        pid = processHandle.pid();
        parendPid = processHandle.parent()
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
        return parendPid;
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
        return CellFactory.tableColumnCellFactory(singletonList((cell, processHandle, value) -> cell.setText(value)));
    }

    public static Callback<TableColumn.CellDataFeatures<ProcessInfo, String>, ObservableValue<String>> tableCellValueFactory() {
        return CellFactory.tableColumnCellValueFactory(features -> {
            switch (features.getTableColumn().getId()) {
                case "identifierColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue())
                            .map(ProcessInfo::pid)
                            .map(Object::toString)
                            .orElse(null));

                case "parentIdentifierColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue())
                            .map(ProcessInfo::parendPid)
                            .filter(pid -> pid >= 0)
                            .map(Object::toString)
                            .orElse(null));

                case "nameColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue())
                            .map(ProcessInfo::name)
                            .orElse(null));

                case "executablePathColumn":
                    return new ReadOnlyStringWrapper(
                        Optional.ofNullable(features.getValue())
                            .map(ProcessInfo::command)
                            .orElse(null));

                default:
                    return null;
            }
        });
    }

}
