/* TagIt
 * ImporterController.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.controllers;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.gui.DynamicCheckTreeView;
import com.github.marcusschmidt4247.tagit.gui.MultiMediaView;
import com.github.marcusschmidt4247.tagit.gui.NameInputDialog;
import com.github.marcusschmidt4247.tagit.gui.TreeViewMenuHandler;
import com.github.marcusschmidt4247.tagit.miscellaneous.FileTypes;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import com.github.marcusschmidt4247.tagit.models.ImporterModel;
import com.github.marcusschmidt4247.tagit.models.TaggerModel;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Vector;

public class ImporterController
{
    @FXML private MultiMediaView mediaView;
    @FXML private DynamicCheckTreeView tagTreeView;

    @FXML private TextField fileNameField;
    private final PseudoClass errorClass = PseudoClass.getPseudoClass("error"); // CSS style class

    @FXML private Label directoryLabel;
    @FXML private Label tagLabel;

    @FXML private Button deselectButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button importButton;
    @FXML private CheckBox copyCheckBox;

    private TaggerModel taggerModel;
    private ImporterModel importerModel;

    public void initialize()
    {
        importerModel = new ImporterModel();

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

        // When the file name changes, check if it's a valid name and assign the CSS error style class if not
        fileNameField.textProperty().addListener((observableValue, s, t1) ->
        {
            String name = fileNameField.getText();
            boolean invalidName = name.isBlank() || !IOManager.validInput(name) || !FileTypes.isSupported(name) || Database.fileExists(taggerModel.getPath(), fileNameField.getText());
            fileNameField.pseudoClassStateChanged(errorClass, invalidName);
        });
    }

    public void setModel(TaggerModel taggerModel)
    {
        if (this.taggerModel == null)
        {
            this.taggerModel = taggerModel;
            mediaView.init(true);

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
            // Menu item to create a new tag
            MenuItem createItem = new MenuItem("Create tag");
            createItem.setOnAction(new TreeViewMenuHandler(taggerModel.getTreeRoot(), tagTreeView, TreeViewMenuHandler.Type.CREATE));
            contextMenu.getItems().add(createItem);
            // Menu item to edit selected tag
            MenuItem editItem = new MenuItem("Edit tag");
            editItem.setOnAction(new TreeViewMenuHandler(taggerModel.getTreeRoot(), tagTreeView, TreeViewMenuHandler.Type.EDIT));
            contextMenu.getItems().add(editItem);
            tagTreeView.setContextMenu(contextMenu);
        }
        else
            throw new IllegalStateException("Model can only be set once");
    }

    @FXML
    public void onManageFolders() { WindowManager.openFolderManager(); }

    @FXML
    public void onSwitchFolders() { WindowManager.switchFolder(mediaView.getScene().getWindow(), taggerModel.getFolder()); }

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
        // Check that there is a current file to import
        if (importerModel.getFiles() != null && !importerModel.getFiles().isEmpty() &&
            importerModel.importIndex >= 0 && importerModel.importIndex < importerModel.getFiles().size())
        {
            // Check that the filename is not blank
            if (fileNameField.getText().isBlank())
                WindowManager.showError("Name cannot be blank");
            // Check that the filename is valid
            else if (!IOManager.validInput(fileNameField.getText()))
                WindowManager.showError("Name cannot contain slashes or quotes");
            else
            {
                // Attempt to import the file to the current managed directory with the name in the TextField
                Path source = Path.of(importerModel.getFiles().get(importerModel.importIndex).getAbsolutePath());
                Path target = Path.of(IOManager.getFilePath(taggerModel.getPath(), fileNameField.getText()));
                importFile(source, target, copyCheckBox.isSelected());
            }
        }
    }

    //******************
    // Private methods *
    //******************

    private void importFile(Path source, Path target, boolean copy)
    {
        String name = target.getFileName().toString();

        // Handle an invalid file extension
        if (!FileTypes.isSupported(name))
        {
            // If it's missing, give the user the option to use the extension in the original file name
            if (FileTypes.getExtension(name) == null)
            {
                String originalExtension = FileTypes.getExtension(importerModel.getFiles().get(importerModel.importIndex).getName());
                String description = "The file name must end with a valid extension. Reapply original file extension?";
                String buttonString = String.format("Use %s", originalExtension);
                if (originalExtension != null && WindowManager.customConfirmationDialog("Error", "Missing file extension", description, buttonString))
                    importFile(source, Path.of(IOManager.getFilePath(taggerModel.getPath(), name.concat(originalExtension))), copy);
            }
            // If it's unsupported, alert the user
            else
                WindowManager.showError("Unsupported file extension");
        }
        // If a file with this name already exists in the managed directory, alert the user and give them the option to rename the new file
        else if (Database.fileExists(taggerModel.getPath(), name))
        {
            String description = String.format("The name must be unique in the \"%s\" folder. Import file with a different name?", taggerModel.getFolder().getName());
            if (WindowManager.customConfirmationDialog("Error", "A file with this name already exists", description, "Rename"))
            {
                // If the user chose to rename the file, open a text input dialog and import the file with the new name
                NameInputDialog dialog = new NameInputDialog(name);
                if (dialog.showAndLoop())
                    importFile(source, Path.of(IOManager.getFilePath(taggerModel.getPath(), dialog.getName())), copy);
            }
        }
        else
        {
            try
            {
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

                // Copy or move the file to the program Storage directory
                if (copy)
                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                else
                    Files.move(source, target);

                // Record this file, its creation time, and the tags being applied to it in the database
                Database.saveFile(taggerModel.getPath(), target.getFileName().toString(), creationTime, importerModel.getAppliedTags());
                importerModel.getFiles().remove(importerModel.importIndex);
            }
            // If a file with this name already exists in the storage directory, alert the user and give them the option to overwrite it
            catch (FileAlreadyExistsException e)
            {
                String description = String.format("A file not tracked by the \"%s\" folder exists in its storage directory with this name. Replace it with the new file?", taggerModel.getFolder().getName());
                if (WindowManager.customConfirmationDialog("Error", "Conflict with unmanaged file", description, "Replace"))
                {
                    // Delete the untracked file
                    try { Files.delete(target); }
                    catch (IOException ex) { throw new RuntimeException(ex); }
                    // Import the new file
                    importFile(source, target, copy);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void refreshContentPane()
    {
        if (importerModel.getFiles() != null && !importerModel.getFiles().isEmpty() &&
            importerModel.importIndex >= 0 && importerModel.importIndex < importerModel.getFiles().size())
        {
            if (fileNameField.isDisabled())
                fileNameField.setDisable(false);
            fileNameField.setText(importerModel.getFiles().get(importerModel.importIndex).getName());
            mediaView.load(importerModel.getFiles().get(importerModel.importIndex));
        }
        else
        {
            fileNameField.setText("");
            fileNameField.setDisable(true);
            mediaView.load(null);
        }
    }
}