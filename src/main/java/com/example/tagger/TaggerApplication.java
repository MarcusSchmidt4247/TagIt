package com.example.tagger;

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
        FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("tagger-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 750, 500);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> ((TaggerController)fxmlLoader.getController()).keyEventHandler(keyEvent));

        stage.setTitle("Tagger");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args)
    {
        launch();
    }
}