/* TagIt
 * ManagedFoldersController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class ManagedFoldersController
{
    @FXML TableView<ManagedFolder> folderTable;
    @FXML TableColumn<ManagedFolder, String> nameColumn;
    @FXML TableColumn<ManagedFolder, String> locationColumn;
    @FXML TableColumn<ManagedFolder, String> isDefaultColumn;

    @FXML Button deleteButton;
    @FXML Button createButton;
    @FXML Button editButton;
    @FXML Button openButton;

    // Create a listener for a list of ManagedFolders that adds or removes them from 'folderTable'
    private final ListChangeListener<ManagedFolder> tableListener = change ->
    {
        while (change.next())
        {
            if (change.wasAdded())
            {
                for (ManagedFolder addedFolder : change.getAddedSubList())
                    folderTable.getItems().add(addedFolder);
            }
            if (change.wasRemoved())
            {
                for (ManagedFolder removedFolder : change.getRemoved())
                    folderTable.getItems().remove(removedFolder);
            }
        }
    };

    private boolean selectionOnly = false;
    private ManagedFolder selection = null;
    public ManagedFolder getSelection() { return selection; }

    public void initialize()
    {
        // Bind the table columns to the ManagedFolder properties
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        locationColumn.setCellValueFactory(cellData -> cellData.getValue().locationProperty());
        isDefaultColumn.setCellValueFactory(cellData -> cellData.getValue().mainFolderProperty());

        // Initialize the table with the current ManagedFolders and add the listener that will keep it up-to-date
        folderTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        folderTable.getItems().addAll(IOManager.getManagedFoldersModel().getManagedFolders());
        IOManager.getManagedFoldersModel().getManagedFolders().addListener(tableListener);
    }

    // Set the window to only allow the selection of a ManagedFolder. 'unavailableFolder' will be removed as an option if it is not null
    public void setSelectionOnly(ManagedFolder unavailableFolder)
    {
        selectionOnly = true;
        deleteButton.setVisible(false);
        createButton.setVisible(false);
        editButton.setVisible(false);
        openButton.setText("Switch");

        if (unavailableFolder != null)
        {
            // Disable the listener that keeps 'folderTable' in sync with the model, and then remove the disallowed folder's row from the table
            IOManager.getManagedFoldersModel().getManagedFolders().removeListener(tableListener);
            folderTable.getItems().remove(unavailableFolder);
        }
    }

    public void onDeleteFolder()
    {
        if (!folderTable.getSelectionModel().isEmpty())
            IOManager.deleteManagedFolder(folderTable.getSelectionModel().getSelectedItem());
    }

    public void onCreateFolder() { WindowManager.openFolderEditor(folderTable.getScene().getWindow(), null); }

    public void onEditFolder()
    {
        if (!folderTable.getSelectionModel().isEmpty())
            WindowManager.openFolderEditor(folderTable.getScene().getWindow(), folderTable.getSelectionModel().getSelectedItem());
    }

    public void onOpenFolder()
    {
        if (!folderTable.getSelectionModel().isEmpty())
        {
            if (selectionOnly)
            {
                selection = folderTable.getSelectionModel().getSelectedItem();
                ((Stage) folderTable.getScene().getWindow()).close();
            }
            else
                WindowManager.openFolder(folderTable.getSelectionModel().getSelectedItem());
        }
    }
}
