/* TagIt
 * IOManager.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit;

import com.github.marcusschmidt4247.tagit.controllers.ManagedFolderEditorController;
import com.github.marcusschmidt4247.tagit.controllers.TagEditorController;
import com.github.marcusschmidt4247.tagit.controllers.TagSelectorController;
import com.github.marcusschmidt4247.tagit.controllers.TaggerController;
import com.github.marcusschmidt4247.tagit.miscellaneous.ExtensionFilter;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import com.github.marcusschmidt4247.tagit.models.ManagedFoldersModel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Vector;

public class IOManager
{
    private static String rootDirectory = null;
    public static String getDefaultDirectoryLocation() { return rootDirectory; }

    private static final String DEFAULT_DIRECTORY_NAME = "Main";
    public static String getDefaultDirectoryName() { return DEFAULT_DIRECTORY_NAME; }

    public static String getDefaultDirectory() { return formatPath(rootDirectory, DEFAULT_DIRECTORY_NAME); }

    private static final String STORAGE_DIRECTORY_NAME = "Storage";
    private static String pathSeparator = null;

    private static Stage managedFoldersStage = null;

    /* Check whether the user program directory, TagIt directory, and default managed directory exist. If not, create them.
     * This method should be called immediately after the application launches, and it can be called again at any time
     * to confirm that these required directories still exist. */
    public static boolean verify()
    {
        // Determine the platform-appropriate path for the user program directory
        String programsDirectoryPath;
        String homePath = System.getProperty("user.home");
        if (System.getProperty("os.name").startsWith("Windows"))
            programsDirectoryPath = formatPath(homePath, "AppData", "Local", "Programs");
        else
            programsDirectoryPath = formatPath(homePath, "Applications");

        // Check whether the user program directory exists, and create it if not
        File programsDirectory = new File(programsDirectoryPath);
        if (!programsDirectory.exists() && !programsDirectory.mkdir())
        {
            System.out.printf("IOManager.verify: User program directory \"%s\" doesn't exist and can't be created\n", programsDirectory.getAbsolutePath());
            showError("User programs directory does not exist");
            return false;
        }

        // Check whether the root TagIt directory exists, and create it if not
        rootDirectory = formatPath(programsDirectoryPath, "TagIt");
        File tagItDirectory = new File(rootDirectory);
        if (!tagItDirectory.exists() && !tagItDirectory.mkdir())
        {
            System.out.println("IOManager.verify: TagIt directory doesn't exist and can't be created");
            showError("Cannot create TagIt directory");
            return false;
        }

        // Check whether the default managed directory exists, and create it if not
        return verify(getDefaultDirectory());
    }

    /* Check whether a directory exists at the given path and that it contains the database and storage subdirectory required to be a managed folder.
     * If any of these elements are missing, they will be created. */
    public static boolean verify(String directoryPath)
    {
        // Check whether the given directory exists, and create it if not
        File directory = new File(directoryPath);
        if (!directory.exists() && !directory.mkdir())
        {
            System.out.printf("IOManager.verify: Directory \"%s\" doesn't exist and can't be created\n", directory.getAbsolutePath());
            showError(String.format("Cannot create managed folder \"%s\"", directory.getName()));
            return false;
        }

        // Check whether the program's storage directory exists, and create it if not
        File storage = new File(formatPath(directoryPath, STORAGE_DIRECTORY_NAME));
        if (!storage.exists() && !storage.mkdir())
        {
            System.out.println("IOManager.verify: Storage directory doesn't exist and can't be created");
            showError("Storage directory does not exist");
            return false;
        }

        // Check whether the program's database file exists (create it if not) and is up-to-date
        File database = new File(formatPath(directoryPath, Database.getName()));
        if (!database.isFile())
        {
            try
            {
                if (database.createNewFile() && Database.createTables(directoryPath))
                {
                    // If a new database file has been created, and it's for the default managed folder, add it to its own database as the primary folder
                    if (directoryPath.equals(getDefaultDirectory()))
                        Database.createManagedFolder(new ManagedFolder(getDefaultDirectoryName(), rootDirectory, true));
                }
                else
                {
                    showError("Cannot create database");
                    return false;
                }
            }
            catch (IOException | SecurityException exception)
            {
                System.out.printf("IOManager.verify: %s\n", exception.toString());
                showError("Cannot create database file");
                return false;
            }
        }
        else if (!Database.isUpToDate(directoryPath))
        {
            showError("Incompatible database version");
            return false;
        }

        return true;
    }

    // Construct a file path from two or more segments using the current operating system's path separator
    public static String formatPath(String rootPath, String ... relativePaths)
    {
        if (rootPath == null)
            return null;
        else
        {
            if (pathSeparator == null)
                pathSeparator = FileSystems.getDefault().getSeparator();

            StringBuilder path = new StringBuilder();
            path.append(rootPath);
            for (String step : relativePaths)
            {
                if (!path.toString().endsWith(pathSeparator))
                    path.append(pathSeparator);
                path.append(step);
            }
            return path.toString();
        }
    }

    public static String getFilePath(String directory, String filename) { return formatPath(directory, STORAGE_DIRECTORY_NAME, filename); }

    public static boolean validInput(String input)
    {
        for (char c : input.toCharArray())
        {
            if (c == '/' || c == '\\' || c == '"' || c == '\'')
                return false;
        }
        return true;
    }

    public static boolean confirmAction(String title, String header, String description)
    {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(title);
        confirmation.setHeaderText(header);
        confirmation.setContentText(description);
        confirmation.setGraphic(null);
        confirmation.getDialogPane().setMaxWidth(400);
        confirmation.showAndWait();
        return (confirmation.getResult() == ButtonType.OK);
    }

    public static void showError(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    // Open a new main window for the default managed folder
    public static void openFolder() { openFolder(DEFAULT_DIRECTORY_NAME, getDefaultDirectory(), new Stage());}

    // Open a new main window for the provided managed folder
    public static void openFolder(ManagedFolder folder) { openFolder(folder, new Stage()); }

    // Open the provided managed folder onto the given stage
    public static void openFolder(ManagedFolder folder, Stage stage) { openFolder(folder.getName(), folder.getFullPath(), stage); }

    // Open the managed folder with the provided directory path onto the given stage
    private static void openFolder(String name, String directory, Stage stage)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("tagger-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 850, 600);
            TaggerController taggerController = fxmlLoader.getController();
            taggerController.setDirectory(directory);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, taggerController::keyEventHandler);

            stage.setTitle(String.format("TagIt - %s", name));
            stage.setScene(scene);
            stage.show();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void manageFolders()
    {
        if (managedFoldersStage == null)
        {
            try
            {
                FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("managed-folders-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                managedFoldersStage = new Stage();
                managedFoldersStage.setTitle("Managed Folders");
                managedFoldersStage.setMinWidth(520);
                managedFoldersStage.setMinHeight(400);
                managedFoldersStage.setScene(scene);
                managedFoldersStage.showAndWait();
                managedFoldersStage = null;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            managedFoldersStage.toFront();
    }

    public static void editManagedFolder(Window owner, ManagedFoldersModel model, ManagedFolder folder)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("managed-folder-editor-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            ((ManagedFolderEditorController) fxmlLoader.getController()).setFolders(model, folder);
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.setMinWidth(400);
            stage.setResizable(false);
            stage.setTitle((folder != null) ? "Edit Folder" : "Create Folder");
            stage.setScene(scene);
            stage.showAndWait();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Create a window to select a tag from the provided TagNode tree (preselectedTag can be null)
    public static TagNode selectTag(TagNode treeRoot, TagNode preselectedTag)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("tag-selector-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            TagSelectorController controller = fxmlLoader.getController();
            controller.setTagNodes(treeRoot, preselectedTag);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Tag Selector");
            stage.setMinWidth(200);
            stage.setMinHeight(200);
            stage.setScene(scene);
            stage.showAndWait();
            return controller.getSelection();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Open a window to edit the provided TagNode
    public static void editTag(Window owner, TagNode tag)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("tag-editor-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            ((TagEditorController) fxmlLoader.getController()).setTag(tag);
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.setResizable(false);
            stage.setTitle("Edit Tag");
            stage.setScene(scene);
            stage.showAndWait();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteTag(TagNode tag)
    {
        // Delete this tag's children first, and halt the process if deleting any of them is unsuccessful
        if (tag.fetchedChildren() && !tag.getChildren().isEmpty())
        {
            for (int i = tag.getChildren().size() - 1; i >= 0; i--)
            {
                if (deleteTag(tag.getChildren().get(i)))
                    return false;
            }
        }

        // Before deleting this tag, check whether there are any files that will be left untagged without it
        boolean proceed = false;
        Vector<String> orphanedFiles = Database.getUniqueFiles(tag);
        if (orphanedFiles.isEmpty())
            proceed = true;
        else
        {
            // If there are files that will be orphaned, the user must choose to cancel, delete the files, or re-tag them
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Orphaned Files");
            warning.setHeaderText(String.format("\"%s\" is the only tag for %d files.", tag.getTag(), orphanedFiles.size()));
            warning.setContentText("These files will be inaccessible without at least one tag. They must either be deleted or moved to another tag.");
            // Get rid of the default button and add three custom  buttons with the appropriate choices
            warning.getButtonTypes().removeFirst();
            warning.getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.RIGHT));
            ButtonType deleteButton = new ButtonType("Delete Files", ButtonBar.ButtonData.RIGHT);
            warning.getButtonTypes().add(deleteButton);
            ButtonType newTagButton = new ButtonType("Select Tag", ButtonBar.ButtonData.RIGHT);
            warning.getButtonTypes().add(newTagButton);
            // Set the button to select a new tag for these files as the default button
            ((Button) warning.getDialogPane().lookupButton(newTagButton)).setDefaultButton(true);
            // Show the dialog and record the user's choice
            warning.showAndWait();

            if (warning.getResult() == deleteButton)
            {
                orphanedFiles.forEach(orphan -> deleteFile(tag.getDirectory(), orphan));
                proceed = true;
            }
            else if (warning.getResult() == newTagButton)
            {
                // If the user chose to re-tag the files, open a tag selector window and re-tag the files in the database with the user's selection
                TagNode selection = IOManager.selectTag(tag.getRoot(), tag);
                if (selection != null)
                {
                    for (String orphan : orphanedFiles)
                    {
                        Database.deleteFileTag(orphan, tag);
                        Database.addFileTag(orphan, selection);
                    }
                    proceed = true;
                }
            }
        }

        // If there was no problem with orphaned files or if the user resolved the problem, delete this tag
        if (proceed)
        {
            Database.deleteTag(tag);
            if (tag.getParent() != null)
                tag.getParent().removeChild(tag);
        }
        return proceed;
    }

    public static boolean deleteManagedFolder(ManagedFoldersModel model, ManagedFolder folder)
    {
        if (!folder.getFullPath().equals(IOManager.getDefaultDirectory()))
        {
            String header = String.format("Are you sure you want to delete the folder \"%s\"?", folder.getName());
            String description = "This will also delete all files and tags managed by this folder. This action cannot be reversed.";
            if (IOManager.confirmAction("Delete Folder", header, description))
            {
                model.getManagedFolders().remove(folder);
                Database.deleteManagedFolder(folder);
                return true;
            }
        }
        else
            IOManager.showError("This folder is required and cannot be deleted.");

        return false;
    }

    /* Return a list of all files in the provided directory that pass the filter (needs a compatible extension),
     * or return null if the path is not a valid directory or does not permit its files to be read */
    public static File[] getFilesInDir(final String PATH)
    {
        File directory = new File(PATH);
        if (directory.isDirectory())
        {
            try
            {
                return directory.listFiles(new ExtensionFilter());
            }
            catch (SecurityException exception)
            {
                System.out.printf("Database.getFilesInDir: Does not have permission to read files in directory \"%s\"", PATH);
                return null;
            }
        }
        else
        {
            System.out.printf("Database.getFilesInDir: Path \"%s\" does not lead to a valid directory", PATH);
            return null;
        }
    }

    public static boolean renameFile(String directory, String oldName, String newName)
    {
        // Check that the new file name isn't already being used
        String targetPath = getFilePath(directory, newName);
        File target = new File(targetPath);
        if (!target.exists())
        {
            // If not, rename the actual file
            String oldPath = getFilePath(directory, oldName);
            File file = new File(oldPath);
            if (file.renameTo(target))
            {
                // Then rename the file in the database
                if (Database.renameFileInDatabase(directory, oldName, newName))
                    return true;
                else
                {
                    // If unable to rename the file in the database, attempt to revert the actual file's name for consistency
                    if (!file.renameTo(new File(oldPath)))
                        System.out.println("IOManager.renameFile: Unable to revert filename");
                }
            }
            else
                System.out.println("IOManager.renameFile: Unable to rename file");
        }
        else
            showError("This file name is already taken");

        return false;
    }

    public static void deleteFile(String directory, String fileName)
    {
        // Delete the actual file from the managed directory
        File file = new File(getFilePath(directory, fileName));
        if (!file.delete())
            System.out.println("IOManager.deleteFile: Unable to delete file");

        // Delete the file's information from the database
        Database.deleteFileFromDatabase(directory, fileName);
    }
}
