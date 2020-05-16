package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.CellFactories
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.util.Callback
import java.nio.file.Paths

class ProcessInfo(processHandle: ProcessHandle) {

    val pid: Long = processHandle.pid()
    private val parentPid: Long? = processHandle.parent().map { it.pid() }.orElse(null)
    val name: String = processHandle.info().command().map { Paths.get(it) }.map { it.fileName }.map { "${it}" }.orElse("?")
    val command: String = processHandle.info().command().orElse("?")

    private val pidProperty: StringProperty = ReadOnlyStringWrapper("${pid}")
    private val parentPidProperty: StringProperty = ReadOnlyStringWrapper(parentPid?.let { "${it}" }?:"")
    private val nameProperty: StringProperty = ReadOnlyStringWrapper(name)
    private val commandProperty: StringProperty = ReadOnlyStringWrapper(command)

    fun destroyForcibly() {
        ProcessHandle.of(pid)
            .ifPresent { it.destroyForcibly() }
    }

    companion object {

        @JvmStatic fun tableCellFactory(): Callback<TableColumn<ProcessInfo?, String?>, TableCell<ProcessInfo?, String?>> {
            return CellFactories.tableColumnCellFactory { cell, column, item, empty -> cell.setText(item) }
        }

        @JvmStatic fun tableCellValueFactory(): Callback<CellDataFeatures<ProcessInfo, String>, ObservableValue<String>> {
            return Callback {
                when (it.tableColumn.id) {
                    "identifierColumn" -> it.value.pidProperty
                    "parentIdentifierColumn" -> it.value.parentPidProperty
                    "nameColumn" -> it.value.nameProperty
                    "executablePathColumn" -> it.value.commandProperty
                        else -> throw IllegalArgumentException("Unknown column: ${it.tableColumn.id}")
                }
            }
        }

    }

}