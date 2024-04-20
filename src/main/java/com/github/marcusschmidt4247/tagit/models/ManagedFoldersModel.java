/* TagIt
 * ManagedFoldersModel.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.models;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

public class ManagedFoldersModel
{
    private final ObservableList<ManagedFolder> managedFolders;
    public ObservableList<ManagedFolder> getManagedFolders() { return managedFolders; }

    private ManagedFolder defaultFolder = null;
    public ManagedFolder getDefaultFolder()
    {
        // If the default folder hasn't been found yet, search for and save it
        if (defaultFolder == null)
        {
            for (ManagedFolder folder : managedFolders)
            {
                if (folder.isDefaultFolder())
                {
                    defaultFolder = folder;
                    break;
                }
            }
        }
        return defaultFolder;
    }

    private ManagedFolder mainFolder = null;
    public ManagedFolder getMainFolder() // might return null!
    {
        // If the main folder is not currently set, search through the list of folders to find it if it exists
        if (mainFolder == null)
        {
            for (ManagedFolder folder : managedFolders)
            {
                if (folder.isMainFolder())
                {
                    // Save this folder and set a listener that will reset 'mainFolder' if it stops being the main folder
                    mainFolder = folder;
                    mainFolder.mainFolderProperty().addListener(new ChangeListener<String>()
                    {
                        @Override
                        public void changed(ObservableValue<? extends String> observableValue, String s, String t1)
                        {
                            mainFolder.mainFolderProperty().removeListener(this);
                            mainFolder = null;
                        }
                    });
                    break;
                }
            }
        }
        return mainFolder;
    }

    public ManagedFoldersModel() { managedFolders = Database.getManagedFolders(); }

    // Add the provided folder to the list of managed folders and the database
    public void addFolder(ManagedFolder newFolder)
    {
        if (newFolder.isMainFolder())
            resetMainFolder();
        managedFolders.add(newFolder);
        Database.createManagedFolder(newFolder);
    }

    // Update the ManagedFolder, database, and directory in device storage with the changes in 'delta'
    public void updateFolder(ManagedFolder folder, ManagedFolder delta)
    {
        // If this change makes the folder the main one, reset the previous main folder
        if (delta.isMainFolder() != null && delta.isMainFolder())
            resetMainFolder();

        // If needed, attempt to move or rename the directory in storage
        boolean update = true;
        if (delta.getName() != null || delta.getLocation() != null)
            update = IOManager.moveManagedFolder(folder, delta);

        // Update the database and ManagedFolder object unless changes in device storage failed
        if (update)
        {
            Database.updateManagedFolder(delta);
            folder.set(delta);
        }
        else
            IOManager.showError("Failed to update folder");
    }

    // Delete the ManagedFolder and its database entry
    public void deleteFolder(ManagedFolder folder)
    {
        folder.delete();
        getManagedFolders().remove(folder);
        Database.deleteManagedFolder(folder);
    }

    // Check if a folder with the given (unique) name exists
    public boolean folderExists(String name)
    {
        for (ManagedFolder folder : managedFolders)
        {
            if (folder.getName().equals(name))
                return true;
        }
        return false;
    }

    // Find the main folder in the list of managed folders and set it to no longer be the main folder (database is updated elsewhere)
    private void resetMainFolder()
    {
        for (ManagedFolder folder : managedFolders)
        {
            if (folder.isMainFolder())
            {
                folder.setMainFolder(false);
                break;
            }
        }
    }
}
