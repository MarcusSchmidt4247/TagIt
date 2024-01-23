package com.example.tagger;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
            populateTagTree((CheckBoxTreeItem<String>) tagTreeView.getRoot(), taggerModel.getTreeRoot().getChildren());

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

    // Add every TagNode in 'children' as a child CheckBoxTreeItem to 'parentItem'
    private void populateTagTree(CheckBoxTreeItem<String> parentItem, Vector<TagNode> children)
    {
        // This function is only expected to be called the first time 'parentItem' is expanded, so get rid of its placeholder child
        if (!parentItem.getChildren().isEmpty() && parentItem.getChildren().getFirst().getValue().equals("invis_cb_tree_item"))
            parentItem.getChildren().removeFirst();

        for (TagNode child : children)
        {
            CheckBoxTreeItem<String> childItem = new CheckBoxTreeItem<>(child.getTag());
            childItem.setIndependent(true);
            /* If this new child is not a leaf node, it needs a listener on its expanded property that will call this method for it,
             * as well as being given a placeholder tree item that will give the user the option to expand it */
            if (!child.isLeaf())
            {
                childItem.expandedProperty().addListener(new ChangeListener<>()
                {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1)
                    {
                        if (observableValue.getValue())
                        {
                            populateTagTree(childItem, child.getChildren());
                            // Stop listening for any further changes now that the children have been added to the tree
                            childItem.expandedProperty().removeListener(this);
                        }
                    }
                });
                CheckBoxTreeItem<String> invisibleItem = new CheckBoxTreeItem<>("invis_cb_tree_item");
                childItem.getChildren().add(invisibleItem);
            }
            parentItem.getChildren().add(childItem);
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