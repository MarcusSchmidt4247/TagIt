/* TagIt
 * ManagedFolderEditorController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;

public class ManagedFolderEditorController
{
    @FXML TextField nameField;
    @FXML Label locationLabel;
    @FXML CheckBox mainCheck;

    @FXML Button locationButton;
    @FXML Button saveButton;
    @FXML Button deleteButton;

    private ManagedFolder folder = null;

    private boolean[] editedProperties;

    public void setFolders(ManagedFolder folder)
    {
        editedProperties = new boolean[3];
        Arrays.fill(editedProperties, false);

        if (folder != null)
        {
            // If a folder was given as an argument, initialize the editable fields with the folder's current state
            this.folder = folder;
            nameField.setText(folder.getName());
            locationLabel.setText(folder.getLocation());
            mainCheck.setSelected(folder.isMainFolder());

            // If the given folder is also the default, disable editing its name/location and the delete button
            if (folder.isDefaultFolder())
            {
                nameField.setDisable(true);
                locationButton.setVisible(false);
                deleteButton.setVisible(false);
            }

            // Add listeners to the name and main folder controls that track whether they have been edited
            nameField.textProperty().addListener(new ChangeListener<>()
            {
                @Override
                public void changed(ObservableValue<? extends String> observableValue, String s, String t1)
                {
                    editedProperties[0] = true;
                    nameField.textProperty().removeListener(this);
                }
            });
            mainCheck.selectedProperty().addListener(new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1)
                {
                    editedProperties[2] = true;
                    mainCheck.selectedProperty().removeListener(this);
                }
            });
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
        {
            locationLabel.setText(result.getAbsolutePath());

            if (!editedProperties[1])
                editedProperties[1] = true;
        }
    }

    public void onDeleteButton()
    {
        if (folder == null)
            System.out.println("ManagedFolderEditorController.onDeleteButton: folder == null");
        else if (IOManager.deleteManagedFolder(folder))
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
        else if ((folder == null || !folder.getName().equals(nameField.getText())) && IOManager.getManagedFoldersModel().folderExists(nameField.getText()))
            IOManager.showError("A managed folder is already using this name");
        else if (locationLabel.getText().isBlank())
            IOManager.showError("A location must be selected");
        else
        {
            File directory = new File(IOManager.formatPath(locationLabel.getText(), nameField.getText()));
            if (folder == null && directory.exists())
                IOManager.showError("A folder with this name already exists at this location");
            else if (folder == null)
            {
                // If a new folder is being created, the chosen directory must be verified before the ManagedFolder object is added to the model
                ManagedFolder newFolder = new ManagedFolder(nameField.getText(), locationLabel.getText(), mainCheck.isSelected());
                if (IOManager.verify(newFolder))
                {
                    IOManager.getManagedFoldersModel().addFolder(newFolder);
                    ((Stage) nameField.getScene().getWindow()).close();
                }
                else
                {
                    System.out.printf("ManagedFolderEditorController.onSaveButton: Unable to verify directory \"%s\"\n", directory.getAbsolutePath());
                    IOManager.showError("Failed to create managed folder at this location");
                }
            }
            else
            {
                // If an existing folder is being edited, one of its properties must have been edited to create a delta object and update the existing object with it
                if (editedProperties[0] || editedProperties[1] || editedProperties[2])
                {
                    ManagedFolder delta = new ManagedFolder();
                    delta.setId(folder.getId());
                    if (editedProperties[0])
                        delta.setName(nameField.getText());
                    if (editedProperties[1])
                        delta.setLocation(locationLabel.getText());
                    if (editedProperties[2])
                        delta.setMainFolder(mainCheck.isSelected());

                    IOManager.getManagedFoldersModel().updateFolder(folder, delta);
                }
                ((Stage) nameField.getScene().getWindow()).close();
            }
        }
    }
}