/* TagIt
 * ManagedFolderEditorController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import com.github.marcusschmidt4247.tagit.models.ManagedFoldersModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class ManagedFolderEditorController
{
    @FXML TextField nameField;
    @FXML Label locationLabel;
    @FXML CheckBox mainCheck;

    @FXML Button locationButton;
    @FXML Button saveButton;
    @FXML Button deleteButton;

    ManagedFoldersModel model = null;
    ManagedFolder folder = null;

    public void setFolders(ManagedFoldersModel model, ManagedFolder folder)
    {
        this.model = model;

        if (folder != null)
        {
            // If a folder was given as an argument, initialize the editable fields with the folder's current state
            this.folder = folder;
            nameField.setText(folder.getName());
            locationLabel.setText(folder.getLocation());
            mainCheck.setSelected(folder.isMainFolder());

            // If the given folder is also the default, disable editing its name/location and the delete button
            if (folder.getFullPath().equals(IOManager.getDefaultDirectory()))
            {
                nameField.setDisable(true);
                locationButton.setVisible(false);
                deleteButton.setVisible(false);
            }
        }
        else
        {
            // If no folder was given as an argument, prepare the window to create a new folder
            locationLabel.setText(IOManager.getDefaultDirectoryLocation());
            saveButton.setText("Create");
            deleteButton.setVisible(false);
        }
    }

    // Open a dialog to choose a directory and update the location label with the result
    public void onSelect()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File(IOManager.getDefaultDirectoryLocation()));
        File result = directoryChooser.showDialog(locationLabel.getScene().getWindow());
        if (result != null)
            locationLabel.setText(result.getAbsolutePath());
    }

    public void onDeleteButton()
    {
        if (model == null)
            System.out.println("ManagedFolderEditorController.onDeleteButton: model == null");
        else if (folder == null)
            System.out.println("ManagedFolderEditorController.onDeleteButton: folder == null");
        else if (IOManager.deleteManagedFolder(model, folder))
            ((Stage) nameField.getScene().getWindow()).close();
    }

    public void onCancelButton() { ((Stage) nameField.getScene().getWindow()).close(); }

    // Verify the current folder information is valid and then create or update it
    public void onSaveButton()
    {
        if (nameField.getText().isBlank())
            IOManager.showError("Name cannot be blank");
        else if (!IOManager.validInput(nameField.getText()))
            IOManager.showError("Name cannot contain slashes or quotes");
        else if ((folder == null || !folder.getName().equals(nameField.getText())) && model.folderExists(nameField.getText()))
            IOManager.showError("A managed folder is already using this name");
        else if (locationLabel.getText().isBlank())
            IOManager.showError("A location must be selected");
        else
        {
            File directory = new File(IOManager.formatPath(locationLabel.getText(), nameField.getText()));
            if (folder == null && directory.exists())
                IOManager.showError("A folder with this name already exists at this location");
            else if (!IOManager.verify(directory.getAbsolutePath()))
                IOManager.showError("Failed to verify folder at this location");
            else
            {
                ManagedFolder newFolder = new ManagedFolder(nameField.getText(), locationLabel.getText(), mainCheck.isSelected());
                if (folder == null)
                    model.addFolder(newFolder);
                else
                    model.updateFolder(folder, newFolder);

                ((Stage) nameField.getScene().getWindow()).close();
            }
        }
    }
}