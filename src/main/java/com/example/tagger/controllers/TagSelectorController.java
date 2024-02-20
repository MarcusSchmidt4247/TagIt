package com.example.tagger.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import com.example.tagger.gui.DynamicCheckTreeView;
import com.example.tagger.miscellaneous.TagNode;

public class TagSelectorController
{
    @FXML
    private DynamicCheckTreeView tagTreeView;

    private TagNode root;
    private TagNode result;

    public void setTagNodes(TagNode root, TagNode tag)
    {
        this.root = root;
        tagTreeView.initSingleCheck(root, tag);
    }

    public void onCancel()
    {
        result = null;
        ((Stage) tagTreeView.getScene().getWindow()).close();
    }

    public void onSelect()
    {
        if (!tagTreeView.getCheckModel().isEmpty())
        {
            TreeItem<String> selection = tagTreeView.getCheckModel().getCheckedItems().getLast();
            result = root.findNode(selection);

            ((Stage) tagTreeView.getScene().getWindow()).close();
        }
        else
        {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setHeaderText("No tag is selected.");
            error.showAndWait();
        }
    }

    public TagNode getSelection() { return result; }
}
