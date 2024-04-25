/* TagIt
 * TagSelectorController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.gui.DynamicCheckTreeView;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;

public class TagSelectorController
{
    @FXML private DynamicCheckTreeView tagTreeView;

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
