package com.example.tagger;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import org.controlsfx.control.CheckTreeView;

import java.io.*;
import java.nio.file.*;
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
    private Label fileNameLabel;
    @FXML
    private Label tagLabel;

    @FXML
    private Button deselectButton;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button importButton;

    private TaggerModel taggerModel;
    private ImporterModel importerModel;

    private static class OffsetDoubleBinding extends DoubleBinding
    {
        private final ObservableDoubleValue PROPERTY;
        private final double OFFSET;
        private double prevResult;

        public OffsetDoubleBinding(ObservableDoubleValue property, final Double OFFSET)
        {
            PROPERTY = property;
            bind(PROPERTY);
            this.OFFSET = OFFSET;
        }

        @Override
        protected double computeValue()
        {
            /* If the bound value is closer to this value than a quarter of the offset, then it must be decreasing
             * quickly and should be given more room to shrink per frame */
            double nextOffset = OFFSET;
            if (PROPERTY.doubleValue() < prevResult + (OFFSET / 4))
                nextOffset *= 3;
            return (prevResult = PROPERTY.doubleValue() - nextOffset);
        }
    }

    public void initialize()
    {
        importerModel = new ImporterModel();

        /* Set the image view to scale with the window. Binding directly to the content pane's width and height leads to
         * a slow, infinite growth because the content pane keeps trying to be slightly bigger because it has a border,
         * so use a custom offset binding. */
        imageView.fitHeightProperty().bind(new OffsetDoubleBinding(contentPane.heightProperty(), 10.0));
        imageView.fitWidthProperty().bind(new OffsetDoubleBinding(contentPane.widthProperty(), 10.0));

        importerModel.getFiles().addListener((ListChangeListener<File>) change ->
        {
            if (importerModel.getFiles() == null || importerModel.getFiles().isEmpty())
            {
                // Since there's nothing in the list, make sure the list-related buttons are disabled
                importButton.setDisable(true);
                prevButton.setDisable(true);
                nextButton.setDisable(true);
            }
            else
            {
                // The list-navigation buttons should be enabled if there's more than one element in the file import list
                if (prevButton.isDisabled() != importerModel.getFiles().size() <= 1)
                {
                    prevButton.setDisable(!prevButton.isDisabled());
                    nextButton.setDisable(!nextButton.isDisabled());
                }
                // The import button should be enabled if there is also at least one tag being applied to the file
                if (importButton.isDisabled() && !importerModel.getAppliedTags().isEmpty())
                    importButton.setDisable(false);

                // Ensure that the current file index is still within bounds
                if (importerModel.importIndex >= importerModel.getFiles().size())
                    importerModel.importIndex = 0;
            }

            refreshContentPane();
        });

        importerModel.getPath().addListener((observableValue, s, t1) ->
        {
            directoryLabel.setText("Importing from: " + importerModel.getPath().getValue());
            importerModel.importIndex = 0;

            // Attempt to set importFiles to hold the files contained in the directory pointed to by IMPORT_PATH
            importerModel.getFiles().clear();
            if (!importerModel.getPath().getValue().isEmpty())
            {
                File[] files = ReadWriteManager.getFilesInDir(importerModel.getPath().getValue());
                if (files != null)
                    importerModel.getFiles().addAll(Arrays.asList(files));
            }
        });
    }

    public void setModel(TaggerModel taggerModel)
    {
        if (this.taggerModel == null)
        {
            this.taggerModel = taggerModel;

            /* Set up the TreeView used for selecting which tags to assign to the imported file.
             * First, assign it a custom cell factory that supports mixing TreeItem and CheckBoxTreeItem nodes so that only
             * leaf nodes can be given a checkbox for tagging purposes.
             * Source for cell factory code: https://blog.idrsolutions.com/mixed-treeview-nodes-javafx/ */
            tagTreeView.setCellFactory(factory -> new CheckBoxTreeCell<>()
            {
                @Override
                public void updateItem(String item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if (empty || item == null)
                    {
                        setText(null);
                        setGraphic(null);
                    }
                    else if (!(getTreeItem() instanceof CheckBoxTreeItem))
                    {
                        setGraphic(null);
                    }
                }
            });
            // Second, recursively traverse the tag data structure to populate the TreeView
            populateTagTree(tagTreeView.getRoot(), taggerModel.getTagTree().getChildren());
            // Finally, add a listener to the list of checked items
            tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                // Update the applied tags list with the changes to the TreeView
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (TreeItem<String> item : change.getAddedSubList())
                        {
                            TagNode addedNode = taggerModel.getTagTree().findNode(item);
                            importerModel.getAppliedTags().add(addedNode);
                        }
                    }
                    if (change.wasRemoved())
                    {
                        for (TreeItem<String> item : change.getRemoved())
                        {
                            TagNode removedNode = taggerModel.getTagTree().findNode(item);
                            importerModel.getAppliedTags().remove(removedNode);
                        }
                    }
                }

                // The deselect button should be enabled if there is at least one CheckBoxTreeItem checked
                if (deselectButton.isDisabled() != tagTreeView.getCheckModel().getCheckedItems().isEmpty())
                    deselectButton.setDisable(!deselectButton.isDisabled());

                // The import button should be enabled if there is at least one file to import and one tag applied to it
                if (importButton.isDisabled() != (importerModel.getAppliedTags().isEmpty() || importerModel.getFiles().isEmpty()))
                    importButton.setDisable(!importButton.isDisabled());

                // Update the label that displays every tag the current file will receive
                StringBuilder stringBuilder = new StringBuilder("This file's tags: ");
                if (!importerModel.getAppliedTags().isEmpty())
                {
                    for (TagNode tag : importerModel.getAppliedTags())
                    {
                        stringBuilder.append(tag.getTagChain()).append(",  ");
                    }
                    stringBuilder.delete(stringBuilder.length() - 3, stringBuilder.length()); // removes the trailing ",  "
                }
                tagLabel.setText(stringBuilder.toString());
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
            importerModel.getPath().set(result.getAbsolutePath());
    }

    @FXML
    public void onDeselectAll()
    {
        tagTreeView.getCheckModel().clearChecks();
    }

    @FXML
    public void onPrevButton()
    {
        if (importerModel.getFiles() != null)
        {
            importerModel.importIndex--;
            if (importerModel.importIndex < 0)
                importerModel.importIndex = importerModel.getFiles().size() - 1;
            refreshContentPane();
        }
    }

    @FXML
    public void onNextButton()
    {
        if (importerModel.getFiles() != null)
        {
            importerModel.importIndex++;
            if (importerModel.importIndex >= importerModel.getFiles().size())
                importerModel.importIndex = 0;
            refreshContentPane();
        }
    }

    @FXML
    public void onImportButton()
    {
        if (importerModel.getFiles() != null && !importerModel.getFiles().isEmpty() &&
                importerModel.importIndex >= 0 && importerModel.importIndex < importerModel.getFiles().size())
        {
            //******************************************************************
            // Copy file to program's Storage directory and add to "files.txt" *
            //******************************************************************
            File importFile = importerModel.getFiles().get(importerModel.importIndex);
            Path source = Path.of(importFile.getAbsolutePath());
            Path target = Path.of(String.format("%s/Storage/%s", taggerModel.getPath(), importFile.getName()));
            try
            {
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                ReadWriteManager.writeNewFileTags(importFile.getName(), importerModel.getAppliedTags(), String.format("%s/files.txt", taggerModel.getPath()));
                importerModel.getFiles().remove(importerModel.importIndex);
            }
            catch (FileAlreadyExistsException e)
            {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setGraphic(null);
                alert.setContentText(String.format("File with name \"%s\" has already been imported", importerModel.getFiles().get(importerModel.importIndex).getName()));
                alert.showAndWait();
                importerModel.getFiles().remove(importerModel.importIndex);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    //******************
    // Private methods *
    //******************

    // A recursive, depth-first approach to populating the CheckTreeView from the custom tree data structure
    private void populateTagTree(TreeItem<String> parent, Vector<TagNode> children)
    {
        for (TagNode node : children)
        {
            if (node.getChildren().isEmpty())
            {
                CheckBoxTreeItem<String> nodeItem = new CheckBoxTreeItem<>(node.getTag());
                parent.getChildren().add(nodeItem);
            }
            else
            {
                TreeItem<String> nodeItem = new TreeItem<>(node.getTag());
                //nodeItem.setExpanded(true);
                parent.getChildren().add(nodeItem);
                populateTagTree(parent.getChildren().getLast(), node.getChildren());
            }
        }
    }

    private void refreshContentPane()
    {
        if (importerModel.getFiles() != null && !importerModel.getFiles().isEmpty() &&
            importerModel.importIndex >= 0 && importerModel.importIndex < importerModel.getFiles().size())
        {
            fileNameLabel.setText(String.format("Current File: %s", importerModel.getFiles().get(importerModel.importIndex).getName()));
            try (FileInputStream input = new FileInputStream(importerModel.getFiles().get(importerModel.importIndex)))
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
            fileNameLabel.setText("Current File:");
            imageView.setVisible(false);
        }
    }
}