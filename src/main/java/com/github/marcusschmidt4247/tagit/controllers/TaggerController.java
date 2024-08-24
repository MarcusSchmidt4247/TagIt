/* TagIt
 * TaggerController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.TaggerApplication;
import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.gui.DynamicCheckTreeView;
import com.github.marcusschmidt4247.tagit.gui.MultiMediaView;
import com.github.marcusschmidt4247.tagit.gui.NameInputDialog;
import com.github.marcusschmidt4247.tagit.miscellaneous.FileTypes;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import com.github.marcusschmidt4247.tagit.miscellaneous.SearchCriteria;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import com.github.marcusschmidt4247.tagit.models.TaggerModel;
import com.github.marcusschmidt4247.tagit.IOManager;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.*;
import java.util.Vector;

public class TaggerController
{
    private final static String SELECT_ALL_TEXT = "Select All";
    private final static String DESELECT_ALL_TEXT = "Deselect All";

    @FXML private Menu fileTypesMenu;
    @FXML private DynamicCheckTreeView tagTreeView;
    @FXML private DynamicCheckTreeView excludeTreeView;
    @FXML private DynamicCheckTreeView editTreeView;
    @FXML private SplitPane mainSplitPane;
    @FXML private AnchorPane editPane;
    @FXML private Label fileNameLabel;
    @FXML private MultiMediaView mediaView;
    @FXML private ChoiceBox<String> criteriaChoiceBox;
    @FXML private ChoiceBox<String> sortChoiceBox;
    @FXML private CheckBox excludeCheckBox;
    @FXML private Button expandButton;
    @FXML private Button includeToggleButton;
    @FXML private Button excludeToggleButton;
    @FXML private Button editNameButton;
    @FXML private Button deleteFileButton;

    private TaggerModel taggerModel;
    private double editPanePos = 0.7;
    private boolean editEnabled = true;

    public void initialize()
    {
        for (FileTypes.Type type : FileTypes.Type.values())
        {
            if (type != FileTypes.Type.UNSUPPORTED)
            {
                CheckMenuItem menuItem = new CheckMenuItem(type.description);
                menuItem.setSelected(true);
                menuItem.setOnAction((eventHandler) -> getCurrentFiles());
                fileTypesMenu.getItems().add(menuItem);
            }
        }

        includeToggleButton.setText(SELECT_ALL_TEXT);
        excludeToggleButton.setText(SELECT_ALL_TEXT);

        // Populate the sort method ChoiceBox, default to the first option, and create a listener to refresh the current files every time it's changed
        for (SearchCriteria.SortMethod method : SearchCriteria.SortMethod.values())
            sortChoiceBox.getItems().add(method.description);
        sortChoiceBox.getSelectionModel().select(0);
        sortChoiceBox.setOnAction(actionEvent -> getCurrentFiles());

        // Set the search criteria ChoiceBox to the first option by default and refresh the current files every time it's changed
        criteriaChoiceBox.getItems().add("Any matching tag");
        criteriaChoiceBox.getItems().add("All matching tags");
        criteriaChoiceBox.getSelectionModel().select(0);
        criteriaChoiceBox.setOnAction(actionEvent -> getCurrentFiles());

        // Refresh the current files every time the exclude tags search criteria is toggled
        excludeCheckBox.selectedProperty().addListener((observableValue, aBoolean, t1) ->
        {
            // If checked but the exclude tree view is hidden, expand it
            if (observableValue.getValue() && !excludeTreeView.isVisible())
                onToggleExcludeView();

            // If there are any excluded tags, refresh the current files with the new exclude state
            if (!excludeTreeView.getCheckModel().getCheckedItems().isEmpty())
                getCurrentFiles();
        });
    }

    public void setFolder(ManagedFolder folder)
    {
        taggerModel = new TaggerModel(folder);

        mediaView.init(false);

        tagTreeView.init(taggerModel.getTreeRoot());
        tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
        {
            Vector<TreeItem<String>> added = new Vector<>();
            Vector<TreeItem<String>> removed = new Vector<>();
            while (change.next())
            {
                added.addAll(change.getAddedSubList());
                removed.addAll(change.getRemoved());
            }
            tagTreeView.processCheckedDelta(added, removed, taggerModel.getTreeRoot(), (node, checked, alt) ->
            {
                // Unless both 'checked' and 'alt' are true, activate or deactivate the node according to 'checked'
                if (!(checked && alt))
                    node.activateNode(checked);
            });

            // Update the label of the "(De)Select All" button if needed
            if (tagTreeView.getCheckModel().isEmpty())
                includeToggleButton.setText(SELECT_ALL_TEXT);
            else if (added.size() == tagTreeView.getCheckModel().getCheckedItems().size())
                includeToggleButton.setText(DESELECT_ALL_TEXT);

            getCurrentFiles();
        });

        excludeTreeView.init(taggerModel.getTreeRoot());
        excludeTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
        {
            Vector<TreeItem<String>> added = new Vector<>();
            Vector<TreeItem<String>> removed = new Vector<>();
            while (change.next())
            {
                added.addAll(change.getAddedSubList());
                removed.addAll(change.getRemoved());
            }
            excludeTreeView.processCheckedDelta(added, removed, taggerModel.getTreeRoot(), (node, checked, alt) ->
            {
                // Unless both 'checked' and 'alt' are true, exclude or stop excluding the node according to 'checked'
                if (!(checked && alt))
                    node.excludeNode(checked);
            });

            // Update the label of the "(De)Select All" button if needed
            if (excludeTreeView.getCheckModel().isEmpty())
                excludeToggleButton.setText(SELECT_ALL_TEXT);
            else if (added.size() == excludeTreeView.getCheckModel().getCheckedItems().size())
                excludeToggleButton.setText(DESELECT_ALL_TEXT);

            // Only update the current list of files if excluded tags are an enabled search criteria
            if (excludeCheckBox.isSelected())
                getCurrentFiles();
        });

        // Initialize and then hide the edit pane
        editTreeView.init(taggerModel.getTreeRoot(), DynamicCheckTreeView.Mode.LEAF_CHECK);
        onToggleEdit();

        // Add a listener that will close the window if the ManagedFolder it views is deleted
        taggerModel.getFolder().deletedProperty().addListener((observableValue, aBoolean, t1) -> ((Stage) mainSplitPane.getScene().getWindow()).close());
    }

    public void keyEventHandler(KeyEvent event)
    {
        event.consume();
        if (event.getCode() == KeyCode.LEFT)
            refreshContentPane(taggerModel.prevFile());
        else if (event.getCode() == KeyCode.RIGHT)
            refreshContentPane(taggerModel.nextFile());
    }

    @FXML
    public void onManageFolders() { WindowManager.openFolderManager(); }

    @FXML
    public void onSwitchFolders() { WindowManager.switchFolder(mainSplitPane.getScene().getWindow(), taggerModel.getFolder()); }

    @FXML
    public void onToggleEdit()
    {
        editEnabled = !editEnabled;
        if (editEnabled)
        {
            // Add the edit pane to the main split pane and restore its divider position
            mainSplitPane.getItems().add(editPane);
            mainSplitPane.setDividerPosition(1, editPanePos);
        }
        else
        {
            // Save the current divider position and remove the edit pane from the main split pane
            editPanePos = mainSplitPane.getDividerPositions()[1];
            mainSplitPane.getItems().remove(editPane);
        }
        refreshEditPane();
    }

    @FXML
    public void onManageFiles()
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("file-manager-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            ((FileManagerController) fxmlLoader.getController()).setModel(taggerModel);
            Stage stage = new Stage();
            stage.setTitle("Files");
            stage.setMinWidth(520);
            stage.setMinHeight(400);
            stage.setScene(scene);
            stage.showAndWait();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onSelectImport() throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("importer-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        ((ImporterController) fxmlLoader.getController()).setModel(taggerModel);

        Stage stage = new Stage();
        stage.setMinWidth(450);
        stage.setMinHeight(400);
        stage.setTitle("File Importer");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void onToggleInclude()
    {
        if (tagTreeView.getCheckModel().isEmpty())
            tagTreeView.getRoot().getChildren().forEach(child -> tagTreeView.getCheckModel().check(child));
        else
            tagTreeView.getCheckModel().clearChecks();
    }

    @FXML
    public void onToggleExclude()
    {
        if (excludeTreeView.getCheckModel().isEmpty())
            excludeTreeView.getRoot().getChildren().forEach(child -> excludeTreeView.getCheckModel().check(child));
        else
            excludeTreeView.getCheckModel().clearChecks();
    }

    @FXML
    public void onToggleExcludeView()
    {
        excludeTreeView.setVisible(!excludeTreeView.isVisible());
        excludeToggleButton.setVisible(excludeTreeView.isVisible());
        String icon = excludeTreeView.isVisible() ? "bxs-down-arrow" : "bxs-right-arrow";
        expandButton.setGraphic(new FontIcon(icon));
    }

    @FXML
    public void onEditFileName()
    {
        NameInputDialog dialog = new NameInputDialog(taggerModel.currentFile());
        if (dialog.showAndLoop())
        {
            if (Database.fileExists(taggerModel.getPath(), dialog.getName()))
                WindowManager.showError("This file name is already taken");
            else
            {
                taggerModel.renameCurrentFile(dialog.getName());
                fileNameLabel.setText(taggerModel.currentFile());
            }
        }
    }

    @FXML
    public void onDeleteFile()
    {
        String header = String.format("Are you sure you want to delete the file \"%s\"?", taggerModel.currentFile());
        if (WindowManager.confirmationDialog("Delete File", header, "This action cannot be reversed."))
        {
            taggerModel.deleteCurrentFile();
            refreshContentPane(taggerModel.currentFile());
        }
    }

    //******************
    // Private methods *
    //******************

    private void getCurrentFiles()
    {
        // Create a list of the file types to search for
        Vector<FileTypes.Type> fileTypes = new Vector<>();
        for (MenuItem menuItem : fileTypesMenu.getItems())
        {
            if (((CheckMenuItem) menuItem).isSelected())
                fileTypes.add(FileTypes.Type.instanceOf(menuItem.getText()));
        }
        // If every type should be searched for, set the list to null for the default (and more efficient) behavior
        if (fileTypes.size() == fileTypesMenu.getItems().size())
            fileTypes = null;

        // Gather the remaining criteria for which files to select
        boolean anyMatch = (criteriaChoiceBox.getSelectionModel().getSelectedIndex() == 0);
        boolean excluding = excludeCheckBox.isSelected();
        SearchCriteria searchCriteria = new SearchCriteria(taggerModel.getTreeRoot(), fileTypes, anyMatch, excluding, getSortMethod());

        // Select files that meet the search criteria and refresh the content pane
        taggerModel.setFiles(Database.getTaggedFiles(searchCriteria));
        refreshContentPane(taggerModel.firstFile());
    }

    // Return a SortMethod instance that corresponds to the item currently selected in the sortChoiceBox control
    private SearchCriteria.SortMethod getSortMethod()
    {
        SearchCriteria.SortMethod[] methods = SearchCriteria.SortMethod.values();
        int index = sortChoiceBox.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < methods.length)
            return methods[index];
        else
            return methods[0];
    }

    private void refreshContentPane(String fileName)
    {
        if (fileName != null)
            mediaView.load(new File(IOManager.getFilePath(taggerModel.getPath(), fileName)));
        else
            mediaView.load(null);

        if (editEnabled)
            refreshEditPane();
    }

    private void refreshEditPane()
    {
        if (taggerModel.currentFile() != null)
        {
            fileNameLabel.setText(taggerModel.currentFile());

            // If the controls are disabled, enable them
            if (editTreeView.isDisable())
            {
                editTreeView.setDisable(false);
                editNameButton.setDisable(false);
                deleteFileButton.setDisable(false);
            }

            // Disable the edit tag tree's checked item listener, set the current file's tags to be checked in the tree, and reapply the listener
            editTreeView.getCheckModel().getCheckedItems().removeListener(editTreeListener);
            editTreeView.getCheckModel().clearChecks();
            editTreeView.checkItems(Database.getFileTags(taggerModel.getTreeRoot(), taggerModel.currentFile()), true);
            editTreeView.getCheckModel().getCheckedItems().addListener(editTreeListener);
        }
        else
        {
            // Remove any previous file data and disable the controls
            fileNameLabel.setText("");
            editTreeView.getCheckModel().clearChecks();
            editTreeView.setDisable(true);
            editNameButton.setDisable(true);
            deleteFileButton.setDisable(true);
        }
    }

    // Create a listener for the edit tag tree's checked item list that adds to or removes from the file's tags as tree items are (un)checked
    private final ListChangeListener<TreeItem<String>> editTreeListener = new ListChangeListener<>()
    {
        @Override
        public void onChanged(Change<? extends TreeItem<String>> change)
        {
            if (taggerModel.currentFile() != null)
            {
                Vector<TreeItem<String>> added = new Vector<>();
                Vector<TreeItem<String>> removed = new Vector<>();
                while (change.next())
                {
                    added.addAll(change.getAddedSubList());
                    removed.addAll(change.getRemoved());
                }
                editTreeView.processCheckedDelta(added, removed, taggerModel.getTreeRoot(), (node, checked, alt) ->
                {
                    if (!alt)
                    {
                        if (checked)
                        {
                            // If this item is the only one checked, it's now safe to delete the file's last tag because a new one is about to be added
                            if (editTreeView.getCheckModel().getCheckedItems().size() == 1)
                            {
                                Vector<TagNode> tags = Database.getFileTags(taggerModel.getTreeRoot(), taggerModel.currentFile());
                                if (!tags.isEmpty())
                                    Database.deleteFileTag(taggerModel.currentFile(), tags.getFirst());
                            }

                            // Add the new file tag
                            Database.addFileTag(taggerModel.currentFile(), node);
                        }
                        else
                        {
                            // Only delete this file's tag if it has at least one more
                            if (Database.getFileTags(taggerModel.getTreeRoot(), taggerModel.currentFile()).size() > 1)
                                Database.deleteFileTag(taggerModel.currentFile(), node);
                            else
                                WindowManager.showError("File will be inaccessible without at least one tag.");
                        }
                    }
                    else
                    {
                        if (checked)
                            editTreeView.getCheckModel().getCheckedItems().addListener(editTreeListener);
                        else
                            editTreeView.getCheckModel().getCheckedItems().removeListener(editTreeListener);
                    }
                });
            }
        }
    };
}