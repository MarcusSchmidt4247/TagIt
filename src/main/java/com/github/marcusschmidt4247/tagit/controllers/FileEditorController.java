/* TagIt
 * FileEditorController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.gui.DynamicCheckTreeView;
import com.github.marcusschmidt4247.tagit.gui.MultiMediaView;
import com.github.marcusschmidt4247.tagit.gui.NameInputDialog;
import com.github.marcusschmidt4247.tagit.miscellaneous.FileTypes;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.util.Vector;

public class FileEditorController
{
    @FXML TextField nameField;
    @FXML MultiMediaView mediaView;
    @FXML DynamicCheckTreeView tagTreeView;
    @FXML Button deselectButton;
    @FXML Button deleteButton;
    @FXML Button cancelButton;
    @FXML Button saveButton;
    @FXML Label newTagLabel;
    @FXML Label delTagLabel;

    private final PseudoClass errorClass = PseudoClass.getPseudoClass("error"); // CSS style class
    private final Vector<TagNode> addedTags = new Vector<>();
    private final Vector<TagNode> removedTags = new Vector<>();

    private String file = null;
    private String directory;
    private int initTagSize;

    public void setFile(String file, TagNode root)
    {
        if (this.file != null)
            System.out.println("FileEditorController.setFile: Method can only be called once");
        else
        {
            this.file = file;
            directory = root.getDirectory();

            // When the file name changes, check if it's a valid name and assign the CSS error style class if not
            nameField.textProperty().addListener((observableValue, s, t1) ->
            {
                String name = nameField.getText();
                boolean invalidName = name.isBlank() || !IOManager.validInput(name) || !FileTypes.isSupported(name) || (!name.equalsIgnoreCase(file) && Database.fileExists(directory, name));
                nameField.pseudoClassStateChanged(errorClass, invalidName);
            });
            nameField.setText(file);

            // Initialize the media viewer and display this file
            mediaView.init(true);
            mediaView.load(new File(IOManager.getFilePath(directory, file)));

            // Initialize the tree and check all this file's tags
            tagTreeView.init(root, DynamicCheckTreeView.Mode.LEAF_CHECK);
            Vector<TagNode> tags = Database.getFileTags(root, file);
            initTagSize = tags.size();
            tagTreeView.checkItems(tags, true);

            // Create a listener to track (un)checked tags and update labels
            tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                Vector<TreeItem<String>> addedItems = new Vector<>();
                Vector<TreeItem<String>> removedItems = new Vector<>();
                while (change.next())
                {
                    addedItems.addAll(change.getAddedSubList());
                    removedItems.addAll(change.getRemoved());
                }

                tagTreeView.processCheckedDelta(addedItems, removedItems, root, (node, checked, alt) ->
                {
                    if (!alt)
                    {
                        // If (un)checking this tag is a new change, add it to the corresponding list. If it undoes a change, remove it from the corresponding list.
                        Vector<TagNode> posList = (checked) ? addedTags : removedTags;
                        Vector<TagNode> negList = (checked) ? removedTags : addedTags;
                        if (negList.contains(node))
                            negList.remove(node);
                        else
                            posList.add(node);
                    }
                    else
                        System.out.printf("FileEditorController.DynamicCheckTreeView.callback: Unhandled alt (checked = %s)\n", checked);
                });

                // Update the deselect button state (should be enabled if there is at least one CheckBoxTreeItem checked)
                if (deselectButton.isDisabled() != tagTreeView.getCheckModel().getCheckedItems().isEmpty())
                    deselectButton.setDisable(!deselectButton.isDisabled());

                // Update the label that lists the added tags to this file if saved
                StringBuilder stringBuilder = new StringBuilder("Added tags: ");
                if (!addedTags.isEmpty())
                {
                    for (TagNode tag : addedTags)
                    {
                        stringBuilder.append(tag.getTagPath()).append(",  ");
                    }
                    stringBuilder.delete(stringBuilder.length() - 3, stringBuilder.length()); // removes the trailing ",  "
                }
                newTagLabel.setText(stringBuilder.toString());

                // Update the label that lists the removed tags to this file if saved
                stringBuilder = new StringBuilder("Removed tags: ");
                if (!removedTags.isEmpty())
                {
                    for (TagNode tag : removedTags)
                    {
                        stringBuilder.append(tag.getTagPath()).append(",  ");
                    }
                    stringBuilder.delete(stringBuilder.length() - 3, stringBuilder.length()); // removes the trailing ",  "
                }
                delTagLabel.setText(stringBuilder.toString());
            });
        }
    }

    @FXML  public void onDeselectAll() { tagTreeView.getCheckModel().clearChecks(); }

    @FXML
    public void onDelete()
    {
        if (file == null)
            System.out.println("FileEditorController.onDelete: setFile() must be called first");
        else
        {
            String header = String.format("Are you sure you want to delete \"%s\"?", file);
            if (WindowManager.confirmationDialog("Delete File", header, "This action cannot be reversed."))
            {
                IOManager.deleteFile(directory, file);
                ((Stage) nameField.getScene().getWindow()).close();
            }
        }
    }

    @FXML public void onCancel() { ((Stage) nameField.getScene().getWindow()).close(); }

    @FXML
    public void onSave()
    {
        if (file == null)
            System.out.println("FileEditorController.onSave: setFile() must be called first");
        else if (removedTags.size() == initTagSize && addedTags.isEmpty())
            WindowManager.showError("File must have at least one tag.");
        else
            saveFile(nameField.getText());
    }

    private void saveFile(String targetName)
    {
        String name = file;
        // If the file is being renamed, validate the new name
        if (!targetName.equals(file))
        {
            name = targetName;
            // Handle an invalid file extension
            if (!FileTypes.isSupported(name))
            {
                // If it's missing, give the user the option to use the extension in the original file name
                if (FileTypes.getExtension(name) == null)
                {
                    String originalExtension = FileTypes.getExtension(file);
                    String description = "The file name must end with a valid extension. Reapply original file extension?";
                    String buttonString = String.format("Use %s", originalExtension);
                    if (originalExtension != null && WindowManager.customConfirmationDialog("Error", "Missing file extension", description, buttonString))
                        name = name.concat(originalExtension);
                    else
                        return;
                }
                // If it's unsupported, alert the user
                else
                {
                    WindowManager.showError("Unsupported file extension");
                    return;
                }
            }

            // If a file with this name already exists in the managed directory, alert the user and give them the option to rename the new file (don't count just changing the case of letters in the old name)
            if (!name.equalsIgnoreCase(file) && Database.fileExists(directory, name))
            {
                String description = "The name must be unique in this folder. Import file with a different name?";
                if (WindowManager.customConfirmationDialog("Error", "A file with this name already exists", description, "Rename"))
                {
                    // If the user chose to rename the file, open a text input dialog and import the file with the new name
                    NameInputDialog dialog = new NameInputDialog(name);
                    if (dialog.showAndLoop())
                        saveFile(dialog.getName());
                }

                return;
            }

            IOManager.renameFile(directory, file, name);
        }

        // Assign new tags to this file
        for (TagNode tag : addedTags)
            Database.addFileTag(name, tag);

        // Remove undesired tags from this file
        for (TagNode tag : removedTags)
            Database.deleteFileTag(name, tag);

        // Close the window
        ((Stage) nameField.getScene().getWindow()).close();
    }
}
