/* TagIt
 * ManagedFoldersModel.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.models;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import javafx.collections.ObservableList;

public class ManagedFoldersModel
{
    private final ObservableList<ManagedFolder> managedFolders;
    public ObservableList<ManagedFolder> getManagedFolders() { return managedFolders; }

    public ManagedFoldersModel() { managedFolders = Database.getManagedFolders(); }

    // Add the provided folder to the list of managed folders and the database
    public void addFolder(ManagedFolder newFolder)
    {
        if (newFolder.isMainFolder())
            resetMainFolder();
        managedFolders.add(newFolder);
        Database.createManagedFolder(newFolder);
    }

    // Update the provided folder with the values of the folderDelta argument
    public void updateFolder(ManagedFolder folder, ManagedFolder folderDelta)
    {
        if (folderDelta.isMainFolder() != null && folderDelta.isMainFolder())
            resetMainFolder();

        folder.set(folderDelta);
        Database.updateManagedFolder(folderDelta);
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
