package com.example.tagger;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    private DynamicCheckTreeView excludeTreeView;
    @FXML
    private AnchorPane contentPane;
    @FXML
    private Label errorLabel;
    @FXML
    private ImageView imageView;
    @FXML
    private RadioButton radioAny;
    @FXML
    private RadioButton radioAll;

    private TaggerModel taggerModel;

    public void initialize()
    {
        // Set the image view to scale with the window
        imageView.fitWidthProperty().bind(contentPane.widthProperty());
        imageView.fitHeightProperty().bind(contentPane.heightProperty());

        // Add the radio buttons to a ToggleGroup to enforce mutual exclusivity and add a listener that will refresh files when a toggle is changed
        ToggleGroup radioButtonGroup = new ToggleGroup();
        radioAny.setToggleGroup(radioButtonGroup);
        radioAll.setToggleGroup(radioButtonGroup);
        radioButtonGroup.selectedToggleProperty().addListener((observableValue, toggle, t1) -> getCurrentFiles());
    }

    public void setModel(TaggerModel taggerModel)
    {
        if (this.taggerModel == null)
        {
            this.taggerModel = taggerModel;

            tagTreeView.init(taggerModel.getTreeRoot());
            tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                while (change.next())
                {
                    if (change.wasAdded())
                        change.getAddedSubList().forEach(item -> taggerModel.getTreeRoot().findNode(item).activateNode(true));
                    if (change.wasRemoved())
                        change.getRemoved().forEach(item -> taggerModel.getTreeRoot().findNode(item).activateNode(false));
                }
                getCurrentFiles();
            });

            excludeTreeView.init(taggerModel.getTreeRoot());
            excludeTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                while (change.next())
                {
                    if (change.wasAdded())
                        change.getAddedSubList().forEach(item -> taggerModel.getTreeRoot().findNode(item).excludeNode(true));
                    if (change.wasRemoved())
                        change.getRemoved().forEach(item -> taggerModel.getTreeRoot().findNode(item).excludeNode(false));
                }
                getCurrentFiles();
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

    @FXML
    public void onDeselectInclude() { tagTreeView.getCheckModel().clearChecks(); }

    @FXML
    public void onDeselectExclude() { excludeTreeView.getCheckModel().clearChecks(); }

    //******************
    // Private methods *
    //******************

    private void getCurrentFiles()
    {
        taggerModel.setFiles(ReadWriteManager.getTaggedFiles(new SearchCriteria(taggerModel.getTreeRoot(), radioAny.isSelected())));
        refreshContentPane(taggerModel.firstFile());
    }

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