/* TagIt
 * ImporterController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.gui.DynamicCheckTreeView;
import com.github.marcusschmidt4247.tagit.gui.NameInputDialog;
import com.github.marcusschmidt4247.tagit.gui.MediaControlView;
import com.github.marcusschmidt4247.tagit.gui.TreeViewMenuHandler;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import com.github.marcusschmidt4247.tagit.models.ImporterModel;
import com.github.marcusschmidt4247.tagit.models.TaggerModel;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Vector;

public class ImporterController
{
    @FXML
    private Pane contentPane;
    @FXML
    private ImageView imageView;
    @FXML
    private MediaControlView mediaView;
    @FXML
    private DynamicCheckTreeView tagTreeView;

    @FXML
    private Label directoryLabel;
    @FXML
    private Label fileNameLabel;
    @FXML
    private Label tagLabel;
    @FXML
    private Label errorLabel;

    @FXML
    private Button deselectButton;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button importButton;

    private TaggerModel taggerModel;
    private ImporterModel importerModel;

    public void initialize()
    {
        importerModel = new ImporterModel();

        /* Set the image view to scale with the window. Binding directly to the content pane's width and height leads to
         * a slow, infinite growth because the content pane keeps trying to be slightly bigger because it has a border,
         * so use a custom offset binding. */
        imageView.fitHeightProperty().bind(new OffsetDoubleBinding(contentPane.heightProperty(), 10.0));
        imageView.fitWidthProperty().bind(new OffsetDoubleBinding(contentPane.widthProperty(), 10.0));

        // Set the media view to scale with the window
        mediaView.fitHeightProperty().bind(new OffsetDoubleBinding(contentPane.heightProperty(), 10.0));
        mediaView.fitWidthProperty().bind(new OffsetDoubleBinding(contentPane.widthProperty(), 10.0));

        importerModel.getFiles().addListener((ListChangeListener<File>) change ->
        {
            if (importerModel.getFiles() == null || importerModel.getFiles().isEmpty())
            {
                // Since there's nothing in the list, make sure the list-related buttons are disabled
                importButton.setDisable(true);
                prevButton.setDisable(true);
                nextButton.setDisable(true);
            }
            else
            {
                // The list-navigation buttons should be enabled if there's more than one element in the file import list
                if (prevButton.isDisabled() != importerModel.getFiles().size() <= 1)
                {
                    prevButton.setDisable(!prevButton.isDisabled());
                    nextButton.setDisable(!nextButton.isDisabled());
                }
                // The import button should be enabled if there is also at least one tag being applied to the file
                if (importButton.isDisabled() && !importerModel.getAppliedTags().isEmpty())
                    importButton.setDisable(false);

                // Ensure that the current file index is still within bounds
                if (importerModel.importIndex >= importerModel.getFiles().size())
                    importerModel.importIndex = 0;
            }

            refreshContentPane();
        });

        importerModel.getPath().addListener((observableValue, s, t1) ->
        {
            directoryLabel.setText("Importing from: " + importerModel.getPath().getValue());
            importerModel.importIndex = 0;

            // Attempt to set importFiles to hold the files contained in the directory pointed to by IMPORT_PATH
            importerModel.getFiles().clear();
            if (!importerModel.getPath().getValue().isEmpty())
            {
                File[] files = IOManager.getFilesInDir(importerModel.getPath().getValue());
                // Adding new files will refresh the content pane, but it also needs to be refreshed even if there aren't any
                if (files != null && files.length > 0)
                    importerModel.getFiles().addAll(Arrays.asList(files));
                else
                    refreshContentPane();
            }
        });
    }

    public void setModel(TaggerModel taggerModel)
    {
        if (this.taggerModel == null)
        {
            this.taggerModel = taggerModel;
            mediaView.init();

            // Initialize and add a listener to the (currently empty) list of checked items
            tagTreeView.init(taggerModel.getTreeRoot(), DynamicCheckTreeView.Mode.LEAF_CHECK);
            tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                Vector<TreeItem<String>> added = new Vector<>();
                Vector<TreeItem<String>> removed = new Vector<>();
                while (change.next())
                {
                    added.addAll(change.getAddedSubList());
                    removed.addAll(change.getRemoved());
                }

                DynamicCheckTreeView.ActionCallback callback = (node, checked, alt) ->
                {
                    // Unless both 'checked' and 'alt' are true, add or remove the associated TagNode from the list of tags that will be applied to a file on import
                    if (!(checked && alt))
                    {
                        if (checked)
                            importerModel.getAppliedTags().add(node);
                        else
                            importerModel.getAppliedTags().remove(node);
                    }
                };

                if (!tagTreeView.processCheckedDelta(added, removed, taggerModel.getTreeRoot(), callback))
                {
                    /* If one of the tags was deleted while checked, then the list of applied tags will be inaccurate.
                     * Reconstruct the list from scratch to keep it up-to-date. */
                    importerModel.getAppliedTags().clear();
                    for (TreeItem<String> item : tagTreeView.getCheckModel().getCheckedItems())
                    {
                        TagNode node = taggerModel.getTreeRoot().findNode(item);
                        if (node != null)
                            importerModel.getAppliedTags().add(node);
                        else
                            System.out.printf("ImporterController.tagTreeView listener: Unable to locate node for \"%s\"\n", item.getValue());
                    }
                }

                // Update the deselect button state (should be enabled if there is at least one CheckBoxTreeItem checked)
                if (deselectButton.isDisabled() != tagTreeView.getCheckModel().getCheckedItems().isEmpty())
                    deselectButton.setDisable(!deselectButton.isDisabled());

                // Update the import button state (should be enabled if there is at least one file to import and one tag applied to it)
                if (importButton.isDisabled() != (importerModel.getAppliedTags().isEmpty() || importerModel.getFiles().isEmpty()))
                    importButton.setDisable(!importButton.isDisabled());

                // Update the label that lists the tags the current file will receive when it is imported
                StringBuilder stringBuilder = new StringBuilder("This file's tags: ");
                if (!importerModel.getAppliedTags().isEmpty())
                {
                    for (TagNode tag : importerModel.getAppliedTags())
                    {
                        stringBuilder.append(tag.getTagPath()).append(",  ");
                    }
                    stringBuilder.delete(stringBuilder.length() - 3, stringBuilder.length()); // removes the trailing ",  "
                }
                tagLabel.setText(stringBuilder.toString());
            });
            ContextMenu contextMenu = new ContextMenu();
            MenuItem createRootItem = new MenuItem("Create root tag");
            createRootItem.setOnAction(new TreeViewMenuHandler(taggerModel.getTreeRoot(), tagTreeView, TreeViewMenuHandler.Type.CREATE_ROOT));
            contextMenu.getItems().add(createRootItem);
            MenuItem createChildItem = new MenuItem("Create child tag");
            createChildItem.setOnAction(new TreeViewMenuHandler(taggerModel.getTreeRoot(), tagTreeView, TreeViewMenuHandler.Type.CREATE_CHILD));
            contextMenu.getItems().add(createChildItem);
            MenuItem editItem = new MenuItem("Edit tag");
            editItem.setOnAction(new TreeViewMenuHandler(taggerModel.getTreeRoot(), tagTreeView, TreeViewMenuHandler.Type.EDIT));
            contextMenu.getItems().add(editItem);
            tagTreeView.setContextMenu(contextMenu);
        }
        else
            throw new IllegalStateException("Model can only be set once");
    }

    @FXML
    public void onManageFolders() { IOManager.manageFolders(); }

    @FXML
    public void onChooseDirectory()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File result = directoryChooser.showDialog(tagTreeView.getScene().getWindow());
        if (result != null)
            importerModel.getPath().set(result.getAbsolutePath());
    }

    @FXML
    public void onDeselectAll()
    {
        tagTreeView.getCheckModel().clearChecks();
    }

    @FXML
    public void onPrevButton()
    {
        if (importerModel.getFiles() != null)
        {
            importerModel.importIndex--;
            if (importerModel.importIndex < 0)
                importerModel.importIndex = importerModel.getFiles().size() - 1;
            refreshContentPane();
        }
    }

    @FXML
    public void onNextButton()
    {
        if (importerModel.getFiles() != null)
        {
            importerModel.importIndex++;
            if (importerModel.importIndex >= importerModel.getFiles().size())
                importerModel.importIndex = 0;
            refreshContentPane();
        }
    }

    @FXML
    public void onImportButton()
    {
        if (importerModel.getFiles() != null && !importerModel.getFiles().isEmpty() &&
                importerModel.importIndex >= 0 && importerModel.importIndex < importerModel.getFiles().size())
        {
            File importFile = importerModel.getFiles().get(importerModel.importIndex);
            Path source = Path.of(importFile.getAbsolutePath());
            Path target = Path.of(IOManager.getFilePath(taggerModel.getPath(), importFile.getName()));
            importFile(source, target);
        }
    }

    //******************
    // Private methods *
    //******************

    private void importFile(Path source, Path target)
    {
        try
        {
            // Copy the file to the program Storage directory
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);

            // Attempt to retrieve the time that this file was created as milliseconds since the epoch
            long creationTime;
            try
            {
                creationTime = Files.readAttributes(source, BasicFileAttributes.class).creationTime().toMillis();
            }
            catch (IOException | UnsupportedOperationException | SecurityException exception)
            {
                System.out.println(exception.toString());
                creationTime = -1;
            }

            // Record this file, its creation time, and the tags being applied to it in the database
            Database.saveFile(taggerModel.getPath(), target.getFileName().toString(), creationTime, importerModel.getAppliedTags());
            importerModel.getFiles().remove(importerModel.importIndex);
        }
        catch (FileAlreadyExistsException e)
        {
            // If a file with this name already exists in the managed directory, alert the user and give them the option to rename the new file
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setContentText(String.format("A file with name \"%s\" already exists", importerModel.getFiles().get(importerModel.importIndex).getName()));
            alert.getButtonTypes().add(ButtonType.CANCEL);
            alert.getButtonTypes().add(new ButtonType("Rename", ButtonBar.ButtonData.RIGHT));
            alert.showAndWait();
            if (alert.getResult() != ButtonType.CANCEL)
            {
                // If the user chose to rename the file, open a text input dialog and import the file with the provided new name
                NameInputDialog dialog = new NameInputDialog(importerModel.getFiles().get(importerModel.importIndex).getName());
                if (dialog.showAndLoop())
                    importFile(source, Path.of(IOManager.getFilePath(taggerModel.getPath(), dialog.getName())));
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void refreshContentPane()
    {
        if (importerModel.getFiles() != null && !importerModel.getFiles().isEmpty() &&
            importerModel.importIndex >= 0 && importerModel.importIndex < importerModel.getFiles().size())
        {
            if (errorLabel.isVisible())
                errorLabel.setVisible(false);

            fileNameLabel.setText(String.format("Current File: %s", importerModel.getFiles().get(importerModel.importIndex).getName()));
            if (importerModel.getFiles().get(importerModel.importIndex).getName().toLowerCase().matches(".+[.](jpe?g|png)$"))
            {
                try (FileInputStream input = new FileInputStream(importerModel.getFiles().get(importerModel.importIndex)))
                {
                    Image image = new Image(input);
                    imageView.setImage(image);

                    if (!imageView.isVisible())
                        imageView.setVisible(true);
                    if (mediaView.isVisible())
                        mediaView.setVisibility(false);
                }
                catch (IOException e)
                {
                    errorLabel.setText(String.format("Unable to load file \"%s\"", importerModel.getFiles().get(importerModel.importIndex).getName()));
                    errorLabel.setVisible(true);
                    imageView.setVisible(false);
                    mediaView.setVisibility(false);
                    throw new RuntimeException(e);
                }
            }
            else if (importerModel.getFiles().get(importerModel.importIndex).getName().toLowerCase().matches(".+[.](mp[34])$"))
            {
                mediaView.load(importerModel.getFiles().get(importerModel.importIndex));

                if (!mediaView.isVisible())
                    mediaView.setVisibility(true);
                if (imageView.isVisible())
                    imageView.setVisible(false);
            }
        }
        else
        {
            errorLabel.setText("No files to import");
            errorLabel.setVisible(true);
            fileNameLabel.setText("Current File:");
            imageView.setVisible(false);
            mediaView.setVisibility(false);
        }
    }

    //******************
    // Private classes *
    //******************

    private static class OffsetDoubleBinding extends DoubleBinding
    {
        private final ObservableDoubleValue PROPERTY;
        private final double OFFSET;
        private double prevResult;

        public OffsetDoubleBinding(ObservableDoubleValue property, final Double OFFSET)
        {
            PROPERTY = property;
            bind(PROPERTY);
            this.OFFSET = OFFSET;
        }

        @Override
        protected double computeValue()
        {
            /* If the bound value is closer to this value than a quarter of the offset, then it must be decreasing
             * quickly and should be given more room to shrink per frame */
            double nextOffset = OFFSET;
            if (PROPERTY.doubleValue() < prevResult + (OFFSET / 4))
                nextOffset *= 3;
            return (prevResult = PROPERTY.doubleValue() - nextOffset);
        }
    }
}