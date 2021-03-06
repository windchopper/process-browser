package com.github.windchopper.tools.process.browser

import com.github.windchopper.common.fx.cdi.form.Form
import jakarta.enterprise.context.ApplicationScoped
import javafx.beans.binding.Bindings
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Dimension2D
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.stage.Screen

@ApplicationScoped @Form(Application.FXML__SELECTION) class SelectionStageController: AnyStageController() {

    @FXML private lateinit var selectionListView: ListView<WindowInfo<*>>
    @FXML private lateinit var selectButton: Button

    override fun preferredStageSize(): Dimension2D {
        return Screen.getPrimary().visualBounds
            .let { Dimension2D(it.width / 3, it.height / 3) }
    }

    override fun afterLoad(form: Parent, parameters: Map<String, *>, formNamespace: Map<String, *>) {
        super.afterLoad(form, parameters, formNamespace)
        stage.title = Application.messages["stage.selection.title"]
        selectButton.disableProperty().bind(Bindings.isNull(selectionListView.selectionModel.selectedItemProperty()))
        parameters["windowHandles"]?.let {
            @Suppress("UNCHECKED_CAST") selectionListView.items.addAll(it as Collection<WindowInfo<*>>)
        }
    }

    fun makeSelectionFullScreen() {
        exceptionally {
            selectionListView.selectionModel.selectedItem.makeFullScreen()
        }
    }

    @FXML fun selectButtonPressed(event: ActionEvent) {
        makeSelectionFullScreen()
    }

    @FXML fun mouseClicked(event: MouseEvent) {
        if (event.button == MouseButton.PRIMARY && event.clickCount > 1) {
            makeSelectionFullScreen()
        }
    }

}