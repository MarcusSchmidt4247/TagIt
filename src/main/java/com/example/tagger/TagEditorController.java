package com.example.tagger;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class TagEditorController
{
    @FXML
    TextField nameField;
    @FXML
    DynamicCheckTreeView tagTreeView;
    @FXML
    Label parentageLabel;

    private TagNode root = null;
    private TagNode node = null;
    private boolean expandedParentage = false;

    public void setTagNodes(TagNode root, TagNode node)
    {
        if (this.root == null)
        {
            this.root = root;
            this.node = node;
            nameField.setText(node.getTag());

            // Set the label showing this tag's parentage
            String parentage = "";
            if (node.getParent() != null && node.getParent().getTagPath() != null)
                parentage = node.getParent().getTagPath();
            parentageLabel.setText(parentage);
            // And then create a listener that will keep the label updated
            tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                if (tagTreeView.getCheckModel().getCheckedItems().isEmpty())
                    parentageLabel.setText("");
                else
                {
                    TagNode lastChecked = root.findNode(tagTreeView.getCheckModel().getCheckedItems().getLast());
                    if (lastChecked == null)
                        parentageLabel.setText("ERROR");
                    else
                        parentageLabel.setText(lastChecked.getTagPath());
                }
            });
        }
    }

    public void onExpand()
    {
        if (!expandedParentage)
        {
            expandedParentage = true;
            tagTreeView.initSingleCheck(root, node);
        }

        tagTreeView.setVisible(!tagTreeView.isVisible());
        tagTreeView.setManaged(!tagTreeView.isManaged());
        tagTreeView.getScene().getWindow().sizeToScene();
    }

    public void onSaveButton()
    {
        if (nameField.getText().isBlank())
        {
            errorDialog("Name cannot be blank");
            return;
        }
        else if (!ReadWriteManager.validInput(nameField.getText()))
        {
            errorDialog("Name cannot contain slashes or quotes");
            return;
        }
        else if (expandedParentage && tagTreeView.getCheckModel().getCheckedItems().isEmpty())
        {
            errorDialog("A parent tag (or the root item) must be selected");
            return;
        }

        // If the name field has been edited, update the name of the node (it has already been checked for validity)
        if (!node.getTag().equals(nameField.getText()))
        {
            node.setTag(nameField.getText());
            ReadWriteManager.renameTag(node);
        }

        if (expandedParentage)
        {
            // Check whether the most recently checked node (should only be one but not enforced in any other way) is a new parent
            TagNode selectedParent = root.findNode(tagTreeView.getCheckModel().getCheckedItems().getLast());
            if (node.getParent() != null && !node.getParent().equals(selectedParent))
            {
                node.changeParent(selectedParent);
                ReadWriteManager.updateTagParentage(node);
            }
        }

        ((Stage) nameField.getScene().getWindow()).close();
    }

    public void onCancelButton()
    {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    public void onDeleteButton()
    {
        node.delete();
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private void errorDialog(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setGraphic(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
