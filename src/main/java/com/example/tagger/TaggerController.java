package com.example.tagger;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.CheckTreeView;

import java.io.*;
import java.util.Vector;

public class TaggerController
{
    @FXML
    private CheckTreeView<String> tagTreeView;
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

            populateTagTree((CheckBoxTreeItem<String>) tagTreeView.getRoot(), taggerModel.getTagTree().getChildren());

            // Docs: https://controlsfx.github.io/javadoc/11.1.0/org.controlsfx.controls/org/controlsfx/control/CheckTreeView.html
            //       https://docs.oracle.com/javase/8/javafx/api/javafx/collections/ListChangeListener.Change.html
            tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                // Get all active tags
                Vector<String> activeTags = new Vector<>();
                for (TreeItem<String> treeItem : tagTreeView.getCheckModel().getCheckedItems())
                    activeTags.add(treeItem.getValue());

                // Gather the names of the files with those tags
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(String.format("%s/files.txt", taggerModel.getPath()))))
                {
                    Vector<String> selectedFiles = new Vector<>();
                    String line = bufferedReader.readLine();
                    while (line != null)
                    {
                        // Regex of expected line format: .+:(.+,)*.+
                        int delimiterIndex = line.indexOf(':');
                        String[] fileTags = line.substring(delimiterIndex + 1).split(",");
                        for (String tag : fileTags)
                        {
                            if (activeTags.contains(tag))
                            {
                                selectedFiles.add(line.substring(0, delimiterIndex));
                                break;
                            }
                        }
                        line = bufferedReader.readLine();
                    }
                    taggerModel.setFiles(selectedFiles);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

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

    // A recursive, depth-first approach to populating the CheckTreeView from the custom tree data structure
    private void populateTagTree(CheckBoxTreeItem<String> parent, Vector<TagNode> children)
    {
        for (TagNode node : children)
        {
            CheckBoxTreeItem<String> nodeItem = new CheckBoxTreeItem<>(node.getTag());
            nodeItem.setIndependent(true);
            parent.getChildren().add(nodeItem);
            if (!node.getChildren().isEmpty())
                populateTagTree((CheckBoxTreeItem<String>) parent.getChildren().getLast(), node.getChildren());
        }
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