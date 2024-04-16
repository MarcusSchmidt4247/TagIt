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
        // Verify the default directory
        if (IOManager.verify())
        {
            // Attempt to retrieve, verify, and open the user's main directory
            ManagedFolder mainFolder = Database.getMainFolder();
            if (mainFolder != null && IOManager.verify(mainFolder.getFullPath()))
                IOManager.openFolder(mainFolder);
            else
            {
                // If unable to open the main directory, then open the default directory
                System.out.println("TaggerApplication.start: Unable to open main window");
                IOManager.openFolder();
            }
        }
    }

    public static void main(String[] args)
    {
        launch();
    }
}