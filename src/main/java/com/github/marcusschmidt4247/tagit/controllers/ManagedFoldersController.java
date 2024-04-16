/* TagIt
 * ManagedFoldersController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import com.github.marcusschmidt4247.tagit.models.ManagedFoldersModel;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ManagedFoldersController
{
    @FXML TableView<ManagedFolder> folderTable;
    @FXML TableColumn<ManagedFolder, String> nameColumn;
    @FXML TableColumn<ManagedFolder, String> locationColumn;
    @FXML TableColumn<ManagedFolder, String> isDefaultColumn;

    ManagedFoldersModel model = null;

    public void initialize()
    {
        model = new ManagedFoldersModel();

        // Bind the table columns to the ManagedFolder properties
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        locationColumn.setCellValueFactory(cellData -> cellData.getValue().locationProperty());
        isDefaultColumn.setCellValueFactory(cellData -> cellData.getValue().mainFolderProperty());

        // Bind the table rows to the list of ManagedFolders
        folderTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        folderTable.itemsProperty().bind(new SimpleObjectProperty<>(model.getManagedFolders()));
    }

    public void onDeleteFolder()
    {
        if (!folderTable.getSelectionModel().isEmpty())
            IOManager.deleteManagedFolder(model, folderTable.getSelectionModel().getSelectedItem());
    }

    public void onCreateFolder() { IOManager.editManagedFolder(folderTable.getScene().getWindow(), model, null); }

    public void onEditFolder()
    {
        if (!folderTable.getSelectionModel().isEmpty())
            IOManager.editManagedFolder(folderTable.getScene().getWindow(), model, folderTable.getSelectionModel().getSelectedItem());
    }

    public void onOpenFolder()
    {
        if (!folderTable.getSelectionModel().isEmpty())
            IOManager.openFolder(folderTable.getSelectionModel().getSelectedItem());
    }
}
