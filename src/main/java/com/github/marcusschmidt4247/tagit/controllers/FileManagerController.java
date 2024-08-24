/* TagIt
 * FileManagerController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.models.TaggerModel;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class FileManagerController
{
    @FXML TextField searchField;
    @FXML ListView<String> filesList;
    @FXML Label numLabel;

    private final ObservableList<String> files = FXCollections.observableArrayList();
    private final FilteredList<String> filteredFiles = new FilteredList<>(files);
    private final Property<FilteredList<String>> filesProperty = new SimpleObjectProperty<>(filteredFiles);

    private TaggerModel model = null;

    public void initialize()
    {
        filteredFiles.predicateProperty().bind(Bindings.createObjectBinding(() -> name -> name.toLowerCase().contains(searchField.getText().toLowerCase()), searchField.textProperty()));
        filteredFiles.addListener((ListChangeListener<String>) change -> numLabel.setText(Integer.toString(filteredFiles.size())));

        filesList.itemsProperty().bind(filesProperty);
    }

    public void setModel(TaggerModel model)
    {
        if (this.model != null)
            System.out.println("FileManagerController.setModel: Method can only be called once");
        else
        {
            this.model = model;
            files.setAll(Database.getFiles(model.getPath()));
        }
    }

    @FXML
    public void onDelete()
    {
        if (model == null)
            System.out.println("FileManagerController.onDelete: setModel() must be called first");
        else
        {
            String file = filesList.getSelectionModel().getSelectedItem();
            String header = String.format("Are you sure you want to delete \"%s\"?", file);
            if (WindowManager.confirmationDialog("Delete File", header, "This action cannot be reversed."))
                IOManager.deleteFile(model.getPath(), file);
        }
    }

    @FXML
    public void onView()
    {
        if (model == null)
            System.out.println("FileManagerController.onView: setModel() must be called first");
        else
            WindowManager.openFileEditor(filesList.getSelectionModel().getSelectedItem(), model.getTreeRoot());
    }
}
