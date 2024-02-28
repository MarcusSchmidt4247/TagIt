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

import com.github.marcusschmidt4247.tagit.controllers.TaggerController;
import com.github.marcusschmidt4247.tagit.models.TaggerModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class TaggerApplication extends Application
{
    @Override
    public void start(Stage stage) throws IOException
    {
        if (IOManager.verify())
        {
            TaggerModel taggerModel = new TaggerModel();

            FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("tagger-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 850, 600);
            TaggerController taggerController = fxmlLoader.getController();
            taggerController.setModel(taggerModel);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, taggerController::keyEventHandler);

            stage.setTitle("TagIt");
            stage.setScene(scene);
            stage.show();
        }
    }

    public static void main(String[] args)
    {
        launch();
    }
}