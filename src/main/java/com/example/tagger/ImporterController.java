package com.example.tagger;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import org.controlsfx.control.CheckTreeView;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;

public class ImporterController
{
    @FXML
    private Pane contentPane;
    @FXML
    private ImageView imageView;
    @FXML
    private CheckTreeView<String> tagTreeView;

    @FXML
    private Label directoryLabel;
    @FXML
    private Label tagLabel;

    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button importButton;

    private final SimpleStringProperty IMPORT_PATH = new SimpleStringProperty();
    private TaggerModel taggerModel;
    private ObservableList<File> importFiles;
    private int importFilesIndex = -1;

    public void initialize()
    {
        // Set the image view to scale with the window
        imageView.fitWidthProperty().bind(contentPane.widthProperty());
        imageView.fitHeightProperty().bind(contentPane.heightProperty());

        importFiles = FXCollections.observableArrayList();
        importFiles.addListener(new ListChangeListener<File>()
        {
            @Override
            public void onChanged(Change<? extends File> change)
            {
                if (importFiles == null || importFiles.isEmpty())
                {
                    // If there's nothing in the list, make sure the list-related buttons are disabled
                    if (!importButton.isDisabled())
                    {
                        importButton.setDisable(true);
                        prevButton.setDisable(true);
                        nextButton.setDisable(true);
                    }
                }
                else
                {
                    // If there's something in the list, make sure the list-related buttons are enabled
                    if (importButton.isDisabled())
                    {
                        importButton.setDisable(false);
                        prevButton.setDisable(false);
                        nextButton.setDisable(false);
                    }

                    // Ensure that the current file index is still within bounds
                    if (importFilesIndex >= importFiles.size())
                        importFilesIndex = 0;
                }

                refreshContentPane();
            }
        });

        IMPORT_PATH.addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1)
            {
                directoryLabel.setText("Importing from: " + IMPORT_PATH.getValue());
                importFilesIndex = 0;

                // Attempt to set importFiles to hold the files contained in the directory pointed to by IMPORT_PATH
                importFiles.clear();
                if (!IMPORT_PATH.getValue().isEmpty())
                {
                    File[] files = ReadWriteManager.getFilesInDir(IMPORT_PATH.getValue());
                    if (files != null)
                        importFiles.addAll(Arrays.asList(files));
                }
            }
        });
    }

    public void setModel(TaggerModel taggerModel)
    {
        if (this.taggerModel == null)
        {
            this.taggerModel = taggerModel;

            // Create the tag tree view from the list of tags in the model, and then set a listener for when tags are checked
            populateTagTree((CheckBoxTreeItem<String>) tagTreeView.getRoot(), taggerModel.getTagTree().getChildren());
            tagTreeView.getCheckModel().getCheckedItems().addListener(new ListChangeListener<TreeItem<String>>()
            {
                @Override
                public void onChanged(Change<? extends TreeItem<String>> change)
                {
                    /*while (change.next())
                    {
                        // etc
                    }*/

                    StringBuilder stringBuilder = new StringBuilder("This file will be tagged with: ");
                    if (!tagTreeView.getCheckModel().getCheckedItems().isEmpty())
                    {
                        for (TreeItem<String> treeItem : tagTreeView.getCheckModel().getCheckedItems())
                        {
                            stringBuilder.append(treeItem.getValue()).append(", ");
                        }
                        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length()); // removes the trailing ", "
                    }
                    tagLabel.setText(stringBuilder.toString());
                }
            });
        }
        else
            throw new IllegalStateException("Model can only be set once");
    }

    @FXML
    public void onChooseDirectory()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        //directoryChooser.setInitialDirectory(new File());
        File result = directoryChooser.showDialog(tagTreeView.getScene().getWindow());
        if (result != null)
            IMPORT_PATH.set(result.getAbsolutePath());
    }

    @FXML
    public void onDeselectAll()
    {
        tagTreeView.getCheckModel().clearChecks();
    }

    @FXML
    public void onPrevButton()
    {
        if (importFiles != null)
        {
            importFilesIndex--;
            if (importFilesIndex < 0)
                importFilesIndex = importFiles.size() - 1;
            refreshContentPane();
        }
    }

    @FXML
    public void onNextButton()
    {
        if (importFiles != null)
        {
            importFilesIndex++;
            if (importFilesIndex >= importFiles.size())
                importFilesIndex = 0;
            refreshContentPane();
        }
    }

    @FXML
    public void onImportButton()
    {
        if (importFiles != null && !importFiles.isEmpty() && importFilesIndex >= 0 && importFilesIndex < importFiles.size())
        {
            // Copy file to program's Storage directory and add to "files.txt"
            importFiles.remove(importFilesIndex);
        }
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
            nodeItem.setExpanded(true);
            parent.getChildren().add(nodeItem);
            if (!node.getChildren().isEmpty())
                populateTagTree((CheckBoxTreeItem<String>) parent.getChildren().getLast(), node.getChildren());
        }
    }

    private void refreshContentPane()
    {
        if (importFiles != null && !importFiles.isEmpty() && importFilesIndex >= 0 && importFilesIndex < importFiles.size())
        {
            try (FileInputStream input = new FileInputStream(importFiles.get(importFilesIndex)))
            {
                Image image = new Image(input);
                imageView.setImage(image);

                if (!imageView.isVisible())
                    imageView.setVisible(true);
            }
            catch (IOException e)
            {
                //errorLabel.setText(String.format("Unable to load file \"%s\"", fileName));
                imageView.setVisible(false);
                throw new RuntimeException(e);
            }
        }
        else
        {
            //errorLabel.setText("No files selected");
            imageView.setVisible(false);
        }
    }
}
