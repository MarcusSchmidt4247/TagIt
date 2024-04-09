/* TagIt
 * IOManager.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit;

import com.github.marcusschmidt4247.tagit.controllers.TagEditorController;
import com.github.marcusschmidt4247.tagit.controllers.TagSelectorController;
import com.github.marcusschmidt4247.tagit.miscellaneous.ExtensionFilter;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;

public class IOManager
{
    private static String rootDirectory = null;
    public static String getRootDirectory() { return rootDirectory; }

    private static String parentDirectory = null;
    private static String storageDirectory = null;
    private static String pathSeparator = null;

    /* Check whether the required program directories and database file exist, creating them if not.
     * This method should be called immediately after the application launches, and can be called again later at any time
     * to confirm that nothing has been deleted. */
    public static boolean verify()
    {
        // Determine the platform-appropriate path for user program directories
        if (parentDirectory == null)
        {
            String homePath = System.getProperty("user.home");
            if (System.getProperty("os.name").startsWith("Windows"))
                parentDirectory = formatPath(homePath, "AppData", "Local", "Programs");
            else
                parentDirectory = formatPath(homePath, "Applications");
        }

        // Create a path for this program's directory in its parent directory
        if (rootDirectory == null)
            rootDirectory = formatPath(parentDirectory, "TagIt");

        if (storageDirectory == null)
            storageDirectory = formatPath(rootDirectory, "Storage");

        // Check whether the parent directory exists, and create it if not
        File parent = new File(parentDirectory);
        if (!parent.exists() && !parent.mkdir())
        {
            System.out.println("IOManager.verify: Parent directory doesn't exist and can't be created");
            showError("User programs directory does not exist");
            return false;
        }

        // Check whether the root program directory exists, and create it if not
        File root = new File(rootDirectory);
        if (!root.exists() && !root.mkdir())
        {
            System.out.println("IOManager.verify: Root directory doesn't exist and can't be created");
            showError("Cannot create TagIt directory");
            return false;
        }

        // Check whether the program's storage directory exists, and create it if not
        File storage = new File(storageDirectory);
        if (!storage.exists() && !storage.mkdir())
        {
            System.out.println("IOManager.verify: Storage directory doesn't exist and can't be created");
            showError("Storage directory does not exist");
            return false;
        }

        // Check whether the program's database file exists (create it if not) and is up-to-date
        File database = new File(formatPath(rootDirectory, Database.getName()));
        if (!database.isFile())
        {
            try
            {
                if (!(database.createNewFile() && Database.createTables()))
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
        else if (!Database.isUpToDate())
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

    public static String getFilePath(String filename)
    {
        return formatPath(storageDirectory, filename);
    }

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

    public static boolean renameFile(String oldName, String newName)
    {
        // Check that the new file name isn't already being used
        String targetPath = getFilePath(newName);
        File target = new File(targetPath);
        if (!target.exists())
        {
            // If not, rename the actual file
            String oldPath = getFilePath(oldName);
            File file = new File(oldPath);
            if (file.renameTo(target))
            {
                // Then rename the file in the database
                if (Database.renameFileInDatabase(oldName, newName))
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

    public static void deleteFile(String fileName)
    {
        // Delete the actual file from the managed directory
        File file = new File(getFilePath(fileName));
        if (!file.delete())
            System.out.println("IOManager.deleteFile: Unable to delete file");

        // Delete the file's information from the database
        Database.deleteFileFromDatabase(fileName);
    }
}
