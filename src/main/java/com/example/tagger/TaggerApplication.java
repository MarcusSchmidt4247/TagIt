package com.example.tagger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.Scanner;
import java.io.IOException;
import java.io.InputStream;

public class TaggerApplication extends Application
{
    @Override
    public void start(Stage stage) throws IOException
    {
        /* Read the directory that stores the program's files from a resource
         * (not final design) */
        String path = "/Applications/tagger";
        try (InputStream input = TaggerApplication.class.getResourceAsStream("program-directory.txt"))
        {
            if (input != null)
                path = new Scanner(input).nextLine();
        }

        // Simple MVC design pattern example: https://stackoverflow.com/questions/32342864
        TaggerModel taggerModel = new TaggerModel(path);

        FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("tagger-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 750, 500);
        TaggerController taggerController = fxmlLoader.getController();
        taggerController.setModel(taggerModel);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, taggerController::keyEventHandler);

        stage.setTitle("Tagger");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args)
    {
        launch();
    }
}