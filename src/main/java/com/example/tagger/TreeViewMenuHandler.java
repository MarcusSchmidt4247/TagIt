package com.example.tagger;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.CheckTreeView;
import java.io.IOException;

public class TreeViewMenuHandler implements EventHandler<ActionEvent>
{
    public enum Type { CREATE, EDIT };

    private final TagNode TREE_ROOT;
    private final CheckTreeView<String> TREE_VIEW;
    private final Type TYPE;

    public TreeViewMenuHandler(TagNode treeRoot, CheckTreeView<String> treeView, Type type)
    {
        TREE_ROOT = treeRoot;
        TREE_VIEW = treeView;
        TYPE = type;
    }

    @Override
    public void handle(ActionEvent actionEvent)
    {
        if (TYPE == Type.CREATE)
            handleCreate();
        else if (TYPE == Type.EDIT)
            handleEdit();
    }

    private void handleCreate()
    {
        // Find the TagNode that is equivalent to the selected TreeItem
        TagNode parent = TREE_ROOT;
        TreeItem<String> selectedItem = TREE_VIEW.getSelectionModel().getSelectedItem();
        if (selectedItem != null)
            parent = TREE_ROOT.findNode(selectedItem);

        // Ask the user to enter a valid name for the new tag
        boolean valid;
        String name = null;
        do
        {
            String instruction = (name == null) ? "Enter the name of the new tag:" : "Cannot contain slashes or quotes,\nand must be unique on this layer\n\nEnter the name of the new tag:";
            name = inputDialog(instruction); // will return null if user cancels
            if (name != null)
                valid = ReadWriteManager.validInput(name) && !parent.hasChild(name);
            else
                valid = false;
        } while (name != null && !valid);

        if (valid)
        {
            // Create a TagNode child for the new tag and add it to the database
            TagNode child = new TagNode(parent, name);
            parent.getChildren().add(child);
            ReadWriteManager.addTag(child);
        }
    }

    private void handleEdit()
    {
        // Find the TagNode that is equivalent to the selected TreeItem
        TreeItem<String> selectedItem = TREE_VIEW.getSelectionModel().getSelectedItem();
        if (selectedItem != null)
        {
            TagNode node = TREE_ROOT.findNode(selectedItem);
            if (node != null)
            {
                try
                {
                    // Create a window to edit this TagNode
                    FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("tag-editor-view.fxml"));
                    Scene scene = new Scene(fxmlLoader.load());
                    ((TagEditorController) fxmlLoader.getController()).setTagNodes(TREE_ROOT, node);
                    Stage stage = new Stage();
                    stage.initOwner(TREE_VIEW.getScene().getWindow());
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.setResizable(false);
                    stage.setTitle("Edit Tag");
                    stage.setScene(scene);
                    stage.show();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            else
                System.out.println("TreeViewMenuHandler.handleEdit: Unable to locate node for selected tree item");
        }
        else
            System.out.println("TreeViewMenuHandler.handleEdit: No selected tree item");
    }

    private String inputDialog(String instruction)
    {
        // Open a dialog that asks the user to input a name for this new tag
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Create Tag");
        nameDialog.setHeaderText(instruction);
        nameDialog.setGraphic(null);
        nameDialog.showAndWait();
        return nameDialog.getResult();
    }
}
