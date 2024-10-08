/* TagIt
 * ManagedFoldersModel.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.models;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

public class ManagedFoldersModel
{
    private final ObservableList<ManagedFolder> managedFolders;
    public ObservableList<ManagedFolder> getManagedFolders() { return managedFolders; }

    private ManagedFolder mainFolder = null;
    /**
     * Gets the <code>ManagedFolder</code> designated as main.
     * @return the folder; <code>null</code> if not found
     */
    public ManagedFolder getMainFolder()
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

    /**
     * Begins tracking and saves a <code>ManagedFolder</code> to the database.
     * @param newFolder the folder to track
     */
    public void addFolder(ManagedFolder newFolder)
    {
        if (newFolder.isMainFolder())
            resetMainFolder();
        managedFolders.add(newFolder);
        Database.createManagedFolder(newFolder);
    }

    /**
     * Updates the <code>ManagedFolder</code> with the values in the delta object. The database and directory in device storage will
     * also be updated to reflect the changes.
     * @param folder the <code>ManagedFolder</code> to update
     * @param delta the object containing new values (must also be assigned original folder ID, any other unchanged properties should be <code>null</code>)
     */
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
            WindowManager.showError("Failed to update folder");
    }

    /**
     * Stops tracking the <code>ManagedFolder</code> object and removes its database entry.
     * @param folder the <code>ManagedFolder</code> to delete
     */
    public void deleteFolder(ManagedFolder folder)
    {
        folder.delete();
        getManagedFolders().remove(folder);
        Database.deleteManagedFolder(folder);
    }

    /**
     * Searches for a <code>ManagedFolder</code> with the given name. The name will be unique if it exists.
     * @param name the folder title to check for
     * @return <code>true</code> if a <code>ManagedFolder</code> with this name exists; <code>false</code> otherwise
     */
    public boolean folderExists(String name)
    {
        for (ManagedFolder folder : managedFolders)
        {
            if (folder.getName().equals(name))
                return true;
        }
        return false;
    }

    /**
     * Removes the main folder's designation. Does not update the database.
     */
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
