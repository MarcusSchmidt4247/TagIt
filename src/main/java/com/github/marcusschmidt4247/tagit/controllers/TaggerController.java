/* TagIt
 * TaggerController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.TaggerApplication;
import com.github.marcusschmidt4247.tagit.gui.DynamicCheckTreeView;
import com.github.marcusschmidt4247.tagit.gui.NameInputDialog;
import com.github.marcusschmidt4247.tagit.gui.MediaControlView;
import com.github.marcusschmidt4247.tagit.miscellaneous.SearchCriteria;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import com.github.marcusschmidt4247.tagit.models.TaggerModel;
import com.github.marcusschmidt4247.tagit.IOManager;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.*;
import java.util.Vector;

public class TaggerController
{
    @FXML
    private DynamicCheckTreeView tagTreeView;
    @FXML
    private DynamicCheckTreeView excludeTreeView;
    @FXML
    private DynamicCheckTreeView editTreeView;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private AnchorPane contentPane;
    @FXML
    private AnchorPane editPane;
    @FXML
    private Label errorLabel;
    @FXML
    private Label fileNameLabel;
    @FXML
    private ImageView imageView;
    @FXML
    private MediaControlView mediaView;
    @FXML
    private ChoiceBox<String> criteriaChoiceBox;
    @FXML
    private ChoiceBox<String> sortChoiceBox;
    @FXML
    private CheckBox excludeCheckBox;
    @FXML
    private Button expandButton;
    @FXML
    private Button deselectButton;
    @FXML
    private Button editNameButton;
    @FXML
    private Button deleteFileButton;

    private TaggerModel taggerModel;
    private double editPanePos = 0.7;
    private boolean editEnabled = true;

    public void initialize()
    {
        // Set the image view to scale with the window
        imageView.fitWidthProperty().bind(contentPane.widthProperty());
        imageView.fitHeightProperty().bind(contentPane.heightProperty());

        // Set the media view to scale with the window
        mediaView.fitHeightProperty().bind(contentPane.heightProperty());
        mediaView.fitWidthProperty().bind(contentPane.widthProperty());

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

    public void setDirectory(String directory)
    {
        taggerModel = new TaggerModel(directory);

        mediaView.init();

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
            // Only update the current list of files if excluded tags are an enabled search criteria
            if (excludeCheckBox.isSelected())
                getCurrentFiles();
        });

        // Initialize and then hide the edit pane
        editTreeView.init(taggerModel.getTreeRoot(), DynamicCheckTreeView.Mode.LEAF_CHECK);
        onToggleEdit();
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
    public void onManageFolders() { IOManager.manageFolders(); }

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
    public void onSelectImport() throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("importer-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        ((ImporterController) fxmlLoader.getController()).setModel(taggerModel);

        Stage stage = new Stage();
        stage.initOwner(contentPane.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setMinWidth(450);
        stage.setMinHeight(400);
        stage.setTitle("File Importer");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void onDeselectInclude() { tagTreeView.getCheckModel().clearChecks(); }

    @FXML
    public void onDeselectExclude() { excludeTreeView.getCheckModel().clearChecks(); }

    @FXML
    public void onToggleExcludeView()
    {
        excludeTreeView.setVisible(!excludeTreeView.isVisible());
        deselectButton.setVisible(excludeTreeView.isVisible());
        String icon = excludeTreeView.isVisible() ? "bxs-down-arrow" : "bxs-right-arrow";
        expandButton.setGraphic(new FontIcon(icon));
    }

    @FXML
    public void onEditFileName()
    {
        NameInputDialog dialog = new NameInputDialog(taggerModel.currentFile());
        if (dialog.showAndLoop())
        {
            taggerModel.renameCurrentFile(dialog.getName());
            fileNameLabel.setText(taggerModel.currentFile());
        }
    }

    @FXML
    public void onDeleteFile()
    {
        String header = String.format("Are you sure you want to delete the file \"%s\"?", taggerModel.currentFile());
        if (IOManager.confirmAction("Delete File", header, "This action cannot be reversed."))
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
        // Gather the criteria for which files to select
        boolean anyMatch = (criteriaChoiceBox.getSelectionModel().getSelectedIndex() == 0);
        boolean excluding = excludeCheckBox.isSelected();
        SearchCriteria searchCriteria = new SearchCriteria(taggerModel.getTreeRoot(), anyMatch, excluding, getSortMethod());

        // Select files that meet the search criteria and refresh the content pane
        taggerModel.setFiles(Database.getTaggedFiles(taggerModel.getDirectory(), searchCriteria));
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
        {
            String filePath = IOManager.getFilePath(taggerModel.getDirectory(), fileName);
            if (fileName.toLowerCase().matches(".+[.](jpe?g|png)$"))
            {
                try (FileInputStream input = new FileInputStream(filePath))
                {
                    Image image = new Image(input);
                    imageView.setImage(image);

                    if (!imageView.isVisible())
                        imageView.setVisible(true);
                    if (mediaView.isVisible())
                        mediaView.setVisibility(false);
                    if (errorLabel.isVisible())
                        errorLabel.setVisible(false);
                }
                catch(IOException e)
                {
                    errorLabel.setText(String.format("Unable to load image file \"%s\"", fileName));
                    errorLabel.setVisible(true);
                    imageView.setVisible(false);
                    mediaView.setVisibility(false);
                }
            }
            else if (fileName.toLowerCase().matches(".+[.](mp[34])$"))
            {
                mediaView.load(new File(filePath));

                if (!mediaView.isVisible())
                    mediaView.setVisibility(true);
                if (imageView.isVisible())
                    imageView.setVisible(false);
                if (errorLabel.isVisible())
                    imageView.setVisible(false);
            }
        }
        else
        {
            errorLabel.setText("No files selected");
            errorLabel.setVisible(true);
            imageView.setVisible(false);
            mediaView.setVisibility(false);
        }

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
            Vector<TagNode> tags = Database.getFileTags(taggerModel.getTreeRoot(), taggerModel.currentFile());
            for (TagNode tag : tags)
            {
                if (tag.isLeaf())
                    ((CheckBoxTreeItem<String>) editTreeView.findItem(tag, true)).setSelected(true);
                else
                    System.out.printf("TaggerController.refreshEditPane: Tag \"%s\" is not a leaf\n", tag.getTag());
            }
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
                                IOManager.showError("File will be inaccessible without at least one tag.");
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