/* TagIt
 * IOManager.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit;

import com.github.marcusschmidt4247.tagit.miscellaneous.ExtensionFilter;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import com.github.marcusschmidt4247.tagit.models.ManagedFoldersModel;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Vector;

public class IOManager
{
    private static String rootDirectory = null;
    public static String getRootDirectory() { return rootDirectory; }

    public static ManagedFolder newDefaultDirectoryObject() { return new ManagedFolder("Main", rootDirectory, true); }

    private static final String STORAGE_DIRECTORY_NAME = "Storage";
    private static String pathSeparator = null;

    private static ManagedFoldersModel managedFoldersModel = null;
    public static ManagedFoldersModel getManagedFoldersModel()
    {
        if (managedFoldersModel == null)
            managedFoldersModel = new ManagedFoldersModel();
        return managedFoldersModel;
    }

    /* Check whether the user program directory, TagIt directory, and root database file exist. If not, create them.
     * This method should be called immediately after the application launches, and it can be called again at any time
     * to confirm that these required files still exist. */
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
            WindowManager.showError("User programs directory does not exist");
            return false;
        }

        // Check whether the root TagIt directory exists, and create it if not
        rootDirectory = formatPath(programsDirectoryPath, "TagIt");
        File tagItDirectory = new File(rootDirectory);
        if (!tagItDirectory.exists() && !tagItDirectory.mkdir())
        {
            System.out.println("IOManager.verify: TagIt directory doesn't exist and can't be created");
            WindowManager.showError("Cannot create TagIt directory");
            return false;
        }

        // Check whether the root database file exists (create it if not) and is up-to-date
        File database = new File(formatPath(rootDirectory, Database.getName()));
        if (!database.isFile())
        {
            try
            {
                if (!(database.createNewFile() && Database.createRootTables(rootDirectory)))
                {
                    WindowManager.showError("Cannot create database");
                    return false;
                }
            }
            catch (IOException | SecurityException exception)
            {
                System.out.printf("IOManager.verify: %s\n", exception.toString());
                WindowManager.showError("Cannot create root database file");
                return false;
            }
        }
        else if (!Database.isUpToDate(rootDirectory, true))
        {
            WindowManager.showError("Incompatible database version");
            return false;
        }

        return true;
    }

    /* Check whether a directory exists at the given path and that it contains the database and storage subdirectory required to be a managed folder.
     * If any of these elements are missing, they will be created. */
    public static boolean verify(ManagedFolder folder)
    {
        String directoryPath = folder.getFullPath();

        // Check whether the given directory exists, and create it if not
        File directory = new File(directoryPath);
        if (!directory.exists() && !directory.mkdir())
        {
            System.out.printf("IOManager.verify: Directory \"%s\" doesn't exist and can't be created\n", directory.getAbsolutePath());
            WindowManager.showError(String.format("Cannot create managed folder \"%s\"", directory.getName()));
            return false;
        }

        // Check whether the program's storage directory exists, and create it if not
        File storage = new File(formatPath(directoryPath, STORAGE_DIRECTORY_NAME));
        if (!storage.exists() && !storage.mkdir())
        {
            System.out.println("IOManager.verify: Storage directory doesn't exist and can't be created");
            WindowManager.showError("Storage directory does not exist");
            return false;
        }

        // Check whether the program's database file exists (create it if not) and is up-to-date
        File database = new File(formatPath(directoryPath, Database.getName()));
        if (!database.isFile())
        {
            try
            {
                if (!(database.createNewFile() && Database.createTables(directoryPath)))
                {
                    WindowManager.showError("Cannot create database");
                    return false;
                }
            }
            catch (IOException | SecurityException exception)
            {
                System.out.printf("IOManager.verify: %s\n", exception.toString());
                WindowManager.showError("Cannot create database file");
                return false;
            }
        }
        else if (!Database.isUpToDate(folder))
        {
            WindowManager.showError("Incompatible database version");
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
                TagNode selection = WindowManager.selectTag(tag.getRoot(), tag);
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

    /* Move a ManagedFolder's corresponding directory in computer storage (NOT in the database).
     * Also used for renaming folders because a directory cannot be renamed if it contains files. */
    public static boolean moveManagedFolder(ManagedFolder folder, ManagedFolder delta)
    {
        // Check that the new directory path isn't already in use
        String targetLocation = (delta.getLocation() == null) ? folder.getLocation() : delta.getLocation();
        String targetName = (delta.getName() == null) ? folder.getName() : delta.getName();
        String targetPath = formatPath(targetLocation, targetName);
        File target = new File(targetPath);
        if (!target.exists())
        {
            // Create the new directory
            if (target.mkdir())
            {
                // Create the new storage subdirectory
                File storage = new File(formatPath(targetPath, STORAGE_DIRECTORY_NAME));
                if (storage.mkdir())
                {
                    try
                    {
                        // Move every file in the original directory to the new one
                        File source = new File(folder.getFullPath());
                        File[] sourceFiles = source.listFiles();
                        if (sourceFiles != null)
                        {
                            for (File file : sourceFiles)
                            {
                                if (file.isFile())
                                    Files.move(file.toPath(), target.toPath().resolve(file.getName()));
                            }
                        }

                        // Move every file in the original storage subdirectory to the new one
                        File sourceStorage = new File(formatPath(folder.getFullPath(), STORAGE_DIRECTORY_NAME));
                        sourceFiles = sourceStorage.listFiles();
                        if (sourceFiles != null)
                        {
                            for (File file : sourceFiles)
                                Files.move(file.toPath(), storage.toPath().resolve(file.getName()));
                        }

                        // Delete the old storage subdirectory
                        if (!sourceStorage.delete())
                            System.out.println("IOManager.moveManagedFolder: Unable to delete original storage directory");

                        // Delete the old directory
                        if (!source.delete())
                            System.out.println("IOManager.moveManagedFolder: Unable to delete original directory");

                        return true;
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else
                    System.out.printf("IOManager.moveManagedFolder: Failed to create storage directory \"%s\"\n", storage.getAbsolutePath());
            }
            else
                System.out.printf("IOManager.moveManagedFolder: Failed to create directory \"%s\"\n", target.getAbsolutePath());
        }
        else
            System.out.printf("IOManager.moveManagedFolder: Destination directory \"%s\" already exists\n", target.getAbsolutePath());

        return false;
    }

    // Confirm with user and then delete a ManagedFolder object, its database entry, and its directory in computer storage
    public static boolean deleteManagedFolder(ManagedFolder folder)
    {
        // Alert the user which data this action deletes and ask for confirmation
        String header = String.format("Are you sure you want to delete the folder \"%s\"?", folder.getName());
        String description = "This will also delete all files and tags managed by this folder. This action cannot be reversed.";
        if (WindowManager.confirmationDialog("Delete Folder", header, description))
        {
            // Delete all the files in the storage subdirectory
            File storage = new File(formatPath(folder.getFullPath(), STORAGE_DIRECTORY_NAME));
            File[] storageFiles = storage.listFiles();
            if (storageFiles != null)
            {
                for (File file : storageFiles)
                {
                    if (!file.delete())
                        System.out.printf("IOManager.deleteManagedFolder: Unable to delete file \"%s\"\n", file.getName());
                }
            }

            // Delete all the files in the folder directory
            File directory = new File(folder.getFullPath());
            File[] files = directory.listFiles();
            if (files != null)
            {
                for (File file : files)
                {
                    if (file.isFile())
                    {
                        if (!file.delete())
                            System.out.printf("IOManager.deleteManagedFolder: Unable to delete file \"%s\"\n", file.getName());
                    }
                }
            }

            // Delete the storage subdirectory
            if (!storage.delete())
                System.out.println("IOManager.deleteManagedFolder: Unable to delete storage directory");

            // Delete the folder directory
            if (!directory.delete())
                System.out.println("IOManager.deleteManagedFolder: Unable to delete directory");

            // Delete the ManagedFolder object and remove it from the database
            getManagedFoldersModel().deleteFolder(folder);

            return true;
        }

        return false;
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
            WindowManager.showError("This file name is already taken");

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
