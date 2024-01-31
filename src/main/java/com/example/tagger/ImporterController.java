package com.example.tagger;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

public class ImporterController
{
    @FXML
    private Pane contentPane;
    @FXML
    private ImageView imageView;
    @FXML
    private DynamicCheckTreeView tagTreeView;

    @FXML
    private Label directoryLabel;
    @FXML
    private Label fileNameLabel;
    @FXML
    private Label tagLabel;
    @FXML
    private Label errorLabel;

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
                // Adding new files will refresh the content pane, but it also needs to be refreshed even if there aren't any
                if (files != null && files.length > 0)
                    importerModel.getFiles().addAll(Arrays.asList(files));
                else
                    refreshContentPane();
            }
        });
    }

    public void setModel(TaggerModel taggerModel)
    {
        if (this.taggerModel == null)
        {
            this.taggerModel = taggerModel;

            // Initialize and add a listener to the (currently empty) list of checked items
            tagTreeView.init(taggerModel.getTreeRoot(), DynamicCheckTreeView.Mode.LEAF_CHECK);
            tagTreeView.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<String>>) change ->
            {
                // When a tree element is (un)checked, add or remove the associated TagNode from the list of tags that will be applied
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (TreeItem<String> item : change.getAddedSubList())
                        {
                            TagNode addedNode = taggerModel.getTreeRoot().findNode(item);
                            if (addedNode != null)
                                importerModel.getAppliedTags().add(addedNode);
                            else
                                System.out.printf("ImporterController.getCheckedItems listener: Unable to find node for added tree item \"%s\"\n", item.getValue());
                        }
                    }
                    if (change.wasRemoved())
                    {
                        for (TreeItem<String> item : change.getRemoved())
                        {
                            TagNode removedNode = taggerModel.getTreeRoot().findNode(item);
                            if (removedNode != null)
                                importerModel.getAppliedTags().remove(removedNode);
                            else
                                System.out.printf("ImporterController.getCheckedItems listener: Unable to find node for removed tree item \"%s\"\n", item.getValue());
                        }
                    }
                }

                // Update the deselect button state (should be enabled if there is at least one CheckBoxTreeItem checked)
                if (deselectButton.isDisabled() != tagTreeView.getCheckModel().getCheckedItems().isEmpty())
                    deselectButton.setDisable(!deselectButton.isDisabled());

                // Update the import button state (should be enabled if there is at least one file to import and one tag applied to it)
                if (importButton.isDisabled() != (importerModel.getAppliedTags().isEmpty() || importerModel.getFiles().isEmpty()))
                    importButton.setDisable(!importButton.isDisabled());

                // Update the label that lists the tags the current file will receive when it is imported
                StringBuilder stringBuilder = new StringBuilder("This file's tags: ");
                if (!importerModel.getAppliedTags().isEmpty())
                {
                    for (TagNode tag : importerModel.getAppliedTags())
                    {
                        stringBuilder.append(tag.getTagPath()).append(",  ");
                    }
                    stringBuilder.delete(stringBuilder.length() - 3, stringBuilder.length()); // removes the trailing ",  "
                }
                tagLabel.setText(stringBuilder.toString());
            });
            ContextMenu contextMenu = new ContextMenu();
            MenuItem createItem = new MenuItem("Create child tag");
            createItem.setOnAction(new TreeViewMenuHandler(taggerModel.getTreeRoot(), tagTreeView, TreeViewMenuHandler.Type.CREATE));
            contextMenu.getItems().add(createItem);
            MenuItem editItem = new MenuItem("Edit tag");
            editItem.setOnAction(new TreeViewMenuHandler(taggerModel.getTreeRoot(), tagTreeView, TreeViewMenuHandler.Type.EDIT));
            contextMenu.getItems().add(editItem);
            tagTreeView.setContextMenu(contextMenu);
        }
        else
            throw new IllegalStateException("Model can only be set once");
    }

    @FXML
    public void onChooseDirectory()
    {
        DirectoryChooser directoryChooser = new DirectoryChooser();
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
                ReadWriteManager.saveFile(taggerModel.getPath(), importFile.getName(), importerModel.getAppliedTags());
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

    private void refreshContentPane()
    {
        if (importerModel.getFiles() != null && !importerModel.getFiles().isEmpty() &&
            importerModel.importIndex >= 0 && importerModel.importIndex < importerModel.getFiles().size())
        {
            if (errorLabel.isVisible())
                errorLabel.setVisible(false);

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
                errorLabel.setText(String.format("Unable to load file \"%s\"", importerModel.getFiles().get(importerModel.importIndex).getName()));
                errorLabel.setVisible(true);
                imageView.setVisible(false);
                throw new RuntimeException(e);
            }
        }
        else
        {
            errorLabel.setText("No files to import");
            errorLabel.setVisible(true);
            fileNameLabel.setText("Current File:");
            imageView.setVisible(false);
        }
    }

    //******************
    // Private classes *
    //******************

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
}