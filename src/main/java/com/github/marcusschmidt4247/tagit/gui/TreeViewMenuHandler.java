/* TagIt
 * TreeViewMenuHandler.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.gui;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import org.controlsfx.control.CheckTreeView;

public class TreeViewMenuHandler implements EventHandler<ActionEvent>
{
    public enum Type { CREATE_ROOT, CREATE_CHILD, EDIT }

    private final TagNode treeRoot;
    private final CheckTreeView<String> treeView;
    private final Type type;

    public TreeViewMenuHandler(TagNode treeRoot, CheckTreeView<String> treeView, Type type)
    {
        this.treeRoot = treeRoot;
        this.treeView = treeView;
        this.type = type;
    }

    @Override
    public void handle(ActionEvent actionEvent)
    {
        if (type == Type.CREATE_ROOT)
            handleCreate(false);
        else if (type == Type.CREATE_CHILD)
            handleCreate(true);
        else if (type == Type.EDIT)
            handleEdit();
        else
            System.out.println("TreeViewMenuHandler.handle: Unrecognized menu item type");
    }

    private void handleCreate(boolean createChild)
    {
        // Find the TagNode that is equivalent to the selected TreeItem
        TagNode parent = treeRoot;
        if (createChild)
        {
            TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
            if (selectedItem != null)
                parent = treeRoot.findNode(selectedItem);
        }

        NameInputDialog dialog = new NameInputDialog("", parent);
        if (dialog.showAndLoop())
        {
            // Create a TagNode child for the new tag and add it to the database
            TagNode child = new TagNode(parent, dialog.getName());
            if (parent.addChild(child))
                Database.addTag(child);
        }
    }

    private void handleEdit()
    {
        // Find the TagNode that is equivalent to the selected TreeItem
        TreeItem<String> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null)
        {
            TagNode node = treeRoot.findNode(selectedItem);
            if (node != null)
                WindowManager.openTagEditor(treeView.getScene().getWindow(), node);
            else
                System.out.println("TreeViewMenuHandler.handleEdit: Unable to locate node for selected tree item");
        }
        else
            System.out.println("TreeViewMenuHandler.handleEdit: No selected tree item");
    }
}
