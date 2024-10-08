/* TagIt
 * IOManager.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit;

import com.github.marcusschmidt4247.tagit.miscellaneous.FileTypes;
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

    /**
     * Verifies that the root <code>TagIt</code> directory and database file exist. If they do not, they will be created.
     * <p/>
     * This method should be called immediately after the application launches. It can be called again at any time.
     * @param customRoot the absolute path to the root <code>TagIt</code> directory; if <code>null</code>, defaults to user program directory
     * @return <code>true</code> if the root directory is valid; <code>false</code> otherwise
     */
    public static boolean verify(String customRoot)
    {
        if (customRoot != null)
            rootDirectory = customRoot;
        else
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

            rootDirectory = formatPath(programsDirectoryPath, "TagIt");
        }

        // Check whether the root TagIt directory exists, and create it if not
        try
        {
            File root = new File(rootDirectory);
            if (!root.exists() && !root.mkdir())
            {
                String directoryType = (customRoot == null) ? "Default root" : "Custom root";
                System.out.printf("IOManager.verify: %s directory doesn't exist and can't be created\n", directoryType);
                WindowManager.showError("Cannot create root directory");
                return false;
            }
        }
        catch (SecurityException securityException)
        {
            System.out.printf("IOManager.verify: SecurityManager has denied permission to custom root directory \"%s\"\n", customRoot);
            rootDirectory = null;
            return verify(rootDirectory);
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

    /**
     * Verifies that all <code>ManagedFolder</code> elements exist. The directory, database, and storage subdirectory will be checked.
     * If any of these elements are missing, they will be created.
     * @param folder the ManagedFolder to verify
     * @return <code>true</code> if the folder is valid; <code>false</code> otherwise
     */
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

    /**
     * Constructs a file path from two or more segments. The path separator for the current OS will be inserted between segments.
     * @param rootPath an absolute file path (must be properly formatted already)
     * @param relativePaths one or more segments to append
     * @return an absolute file path
     */
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

    /**
     * Constructs the complete path to a file.
     * @param directory the absolute path to the file's <code>ManagedFolder</code> directory
     * @param filename the name of the file
     * @return an absolute file path
     */
    public static String getFilePath(String directory, String filename) { return formatPath(directory, STORAGE_DIRECTORY_NAME, filename); }

    /**
     * Searches a string for characters that are not allowed.
     * @param input the string to validate
     * @return <code>true</code> if <code>input</code> uses only valid characters; <code>false</code> otherwise
     */
    public static boolean validInput(String input)
    {
        for (char c : input.toCharArray())
        {
            if (c == '/' || c == '\\' || c == '"' || c == '\'')
                return false;
        }
        return true;
    }

    /**
     * Safely deletes a tag from the database. All children tags will be deleted first, and each one will be checked for any files that will
     * be inaccessible  without it, giving the user a chance to resolve the conflict or halt the process.
     * @param tag the <code>TagNode</code> to delete
     * @return <code>true</code> if the <code>TagNode</code> was deleted; <code>false</code> otherwise
     */
    public static boolean deleteTag(TagNode tag)
    {
        // Delete this tag's children first, and halt the process if deleting any of them is unsuccessful
        if (tag.fetchedChildren() && !tag.getChildren().isEmpty())
        {
            for (int i = tag.getChildren().size() - 1; i >= 0; i--)
            {
                if (!deleteTag(tag.getChildren().get(i)))
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

    /**
     * Fetches all files in a directory that have a supported file extension. Should not be used on a <code>ManagedFolder</code> directory.
     * @param PATH the absolute path to a file directory
     * @return an array of file names; <code>null</code> if path is invalid or directory does not permit its files to be read
     */
    public static File[] getFilesInDir(final String PATH)
    {
        File directory = new File(PATH);
        if (directory.isDirectory())
        {
            try { return directory.listFiles((dir, name) -> FileTypes.isSupported(name)); }
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

    /**
     * Moves a <code>ManagedFolder</code> directory to the location in <code>delta</code>. The database entry and <code>folder</code>
     * object will not be changed.
     * <p/>
     * If <code>delta</code> contains a name, this method can also be used to rename the <code>ManagedFolder</code> directory.
     * @param folder the target to move its directory and files
     * @param delta a container for the new location (and optionally a new name)
     * @return <code>true</code> if moved; <code>false</code> otherwise
     */
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

    /**
     * Deletes <code>ManagedFolder</code> files and directory from device storage. Receives permission from user before deleting. Deletes
     * <code>folder</code> from <code>ManagedFoldersModel</code> at the end.
     * @param folder the <code>ManagedFolder</code> to delete
     * @return <code>true</code> if deleted; <code>false</code> otherwise
     */
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

    /**
     * Renames a file in device storage and the database.
     * @param directory the absolute path to the file's <code>ManagedFolder</code> directory
     * @param oldName the current file name
     * @param newName the name to assign to the file
     * @return <code>true</code> if the file was renamed; <code>false</code> otherwise
     */
    public static boolean renameFile(String directory, String oldName, String newName)
    {
        // Check that the new file name isn't already being used (don't count this file if only character cases are being changed)
        String targetPath = getFilePath(directory, newName);
        File target = new File(targetPath);
        if (newName.equalsIgnoreCase(oldName) || !target.exists())
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
            WindowManager.showError("A file with this name already exists");

        return false;
    }

    /**
     * Deletes a file from device storage and the database.
     * @param directory the absolute path to the directory
     * @param fileName the name of the file
     */
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
