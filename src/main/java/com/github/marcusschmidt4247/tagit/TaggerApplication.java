/* TagIt
 * TaggerApplication.java
 * Copyright (C) 2024  Marcus Schmidt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>. */

package com.github.marcusschmidt4247.tagit;

import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import javafx.application.Application;
import javafx.stage.Stage;

public class TaggerApplication extends Application
{
    @Override
    public void start(Stage stage)
    {
        // Verify the root directory
        if (IOManager.verify())
        {
            // If there aren't any managed folders, attempt to create, verify, and open the default folder
            if (IOManager.getManagedFoldersModel().getManagedFolders().isEmpty())
            {
                ManagedFolder defaultFolder = IOManager.newDefaultDirectoryObject();
                if (IOManager.verify(defaultFolder))
                {
                    IOManager.getManagedFoldersModel().addFolder(defaultFolder);
                    IOManager.openFolder(defaultFolder, stage);
                }
                else
                    System.out.println("TaggerApplication.start: Failed to verify default folder");
            }
            else
            {
                // If there are managed folders, attempt to get the user's main folder (if there isn't one, default to the first folder in the list)
                ManagedFolder folder = IOManager.getManagedFoldersModel().getMainFolder();
                if (folder == null)
                {
                    folder = IOManager.getManagedFoldersModel().getManagedFolders().getFirst();
                    System.out.printf("TaggerApplication.start: No main folder, defaulting to \"%s\"\n", folder.getName());
                }

                if (IOManager.verify(folder))
                    IOManager.openFolder(folder, stage);
                else
                    System.out.println("TaggerApplication.start: Failed to verify folder");
            }
        }
    }

    public static void main(String[] args)
    {
        launch();
    }
}