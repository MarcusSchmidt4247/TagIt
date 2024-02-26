package com.example.tagger;

import com.example.tagger.controllers.TaggerController;
import com.example.tagger.models.TaggerModel;
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
            // Simple MVC design pattern example: https://stackoverflow.com/questions/32342864
            TaggerModel taggerModel = new TaggerModel();

            FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("tagger-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 850, 600);
            TaggerController taggerController = fxmlLoader.getController();
            taggerController.setModel(taggerModel);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, taggerController::keyEventHandler);

            stage.setTitle("Tagger");
            stage.setScene(scene);
            stage.show();
        }
    }

    public static void main(String[] args)
    {
        launch();
    }
}