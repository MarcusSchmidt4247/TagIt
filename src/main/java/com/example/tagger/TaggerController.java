package com.example.tagger;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;

public class TaggerController
{
    @FXML
    private DynamicCheckTreeView tagTreeView;
    @FXML
    private AnchorPane contentPane;
    @FXML
    private Label errorLabel;
    @FXML
    private ImageView imageView;

    private TaggerModel taggerModel;

    public void initialize()
    {
        // Set the image view to scale with the window
        imageView.fitWidthProperty().bind(contentPane.widthProperty());
        imageView.fitHeightProperty().bind(contentPane.heightProperty());
    }

    public void setModel(TaggerModel taggerModel)
    {
        if (this.taggerModel == null)
        {
            this.taggerModel = taggerModel;
            tagTreeView.init(taggerModel.getTreeRoot());

            // Docs: https://controlsfx.github.io/javadoc/11.1.0/org.controlsfx.controls/org/controlsfx/control/CheckTreeView.html
            //       https://docs.oracle.com/javase/8/javafx/api/javafx/collections/ListChangeListener.Change.html
            tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (TreeItem<String> item : change.getAddedSubList())
                        {
                            taggerModel.getTreeRoot().findNode(item).activateNode();
                        }
                    }
                    if (change.wasRemoved())
                    {
                        for (TreeItem<String> item : change.getRemoved())
                        {
                            taggerModel.getTreeRoot().findNode(item).deactivateNode();
                        }
                    }
                }

                // Refresh currently selected files and the content pane displaying them
                taggerModel.setFiles(ReadWriteManager.getTaggedFiles(taggerModel.getTreeRoot()));
                refreshContentPane(taggerModel.firstFile());
            });
        }
        else
            throw new IllegalStateException("Model can only be set once");
    }

    public void keyEventHandler(KeyEvent event)
    {
        event.consume();
        if (event.getCode() == KeyCode.LEFT)
            refreshContentPane(taggerModel.prevFile());
        else if (event.getCode() == KeyCode.RIGHT)
            refreshContentPane(taggerModel.nextFile());
    }

    @FXML
    public void onSelectImport() throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("importer-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        ((ImporterController) fxmlLoader.getController()).setModel(taggerModel);

        Stage stage = new Stage();
        stage.initOwner(contentPane.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setMinWidth(450);
        stage.setMinHeight(400);
        stage.setTitle("File Importer");
        stage.setScene(scene);
        stage.show();
    }

    //******************
    // Private methods *
    //******************

    private void refreshContentPane(String fileName)
    {
        if (fileName != null)
        {
            String filePath = String.format("%s/Storage/%s", taggerModel.getPath(), fileName);
            try (FileInputStream input = new FileInputStream(filePath))
            {
                Image image = new Image(input);
                imageView.setImage(image);

                if (!imageView.isVisible())
                    imageView.setVisible(true);
            }
            catch (IOException e)
            {
                errorLabel.setText(String.format("Unable to load file \"%s\"", fileName));
                imageView.setVisible(false);
                throw new RuntimeException(e);
            }
        }
        else
        {
            errorLabel.setText("No files selected");
            imageView.setVisible(false);
        }
    }
}