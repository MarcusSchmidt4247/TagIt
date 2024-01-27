package com.example.tagger;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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

            /* Set up the TreeView used for selecting which tags to assign to the imported file.
             * First, it needs a custom cell factory that supports mixing TreeItem and CheckBoxTreeItem nodes so that only
             * leaf nodes have a checkbox for tagging purposes.
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
            // Add the top-level tags to the tree view
            populateTagTree(tagTreeView.getRoot(), taggerModel.getTreeRoot().getChildren());
            // Finally, add a listener to the (currently empty) list of checked items
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
                            importerModel.getAppliedTags().add(addedNode);
                        }
                    }
                    if (change.wasRemoved())
                    {
                        for (TreeItem<String> item : change.getRemoved())
                        {
                            TagNode removedNode = taggerModel.getTreeRoot().findNode(item);
                            importerModel.getAppliedTags().remove(removedNode);
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
            createItem.setOnAction(new TreeViewMenuHandler(taggerModel.getTreeRoot(), tagTreeView));
            contextMenu.getItems().add(createItem);
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

    // Add every TagNode in 'children' as a child TreeItem to 'parentItem'
    private void populateTagTree(TreeItem<String> parentItem, ObservableList<TagNode> children)
    {
        // This function is only expected to be called the first time 'parentItem' is expanded, so get rid of its placeholder child
        if (!parentItem.getChildren().isEmpty() && parentItem.getChildren().getFirst().getValue().equals("invis_cb_tree_item"))
            parentItem.getChildren().removeFirst();

        for (TagNode child : children)
        {
            if (!child.isLeaf())
            {
                /* If this new child is not a leaf node, it needs a listener on its expanded property that will call this method for it,
                 * as well as being given a placeholder tree item that will give the user the option to expand it */
                TreeItem<String> childItem = new TreeItem<>(child.getTag());
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
                parentItem.getChildren().add(childItem);
            }
            else
            {
                // If this new child is a leaf node, it can simply be added to the CheckTreeView
                CheckBoxTreeItem<String> nodeItem = new CheckBoxTreeItem<>(child.getTag());
                parentItem.getChildren().add(nodeItem);
            }
        }
    }

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

    private static class TreeViewMenuHandler implements EventHandler<ActionEvent>
    {
        private final TagNode TREE_ROOT;
        private final CheckTreeView<String> TREE_VIEW;

        public TreeViewMenuHandler(TagNode treeRoot, CheckTreeView<String> treeView)
        {
            TREE_ROOT = treeRoot;
            TREE_VIEW = treeView;
        }

        @Override
        public void handle(ActionEvent actionEvent)
        {
            // Find the TagNode that is equivalent to the selected TreeItem
            TagNode parent = TREE_ROOT;
            TreeItem<String> selectedItem = TREE_VIEW.getSelectionModel().getSelectedItem();
            if (selectedItem != null)
                parent = TREE_ROOT.findNode(selectedItem);

            // Ask the user to enter a valid name for the new tag
            boolean valid;
            String name = null;
            do
            {
                String instruction = (name == null) ? "Enter the name of the new tag:" : "Cannot contain slashes or quotes,\nand must be unique on this layer\n\nEnter the name of the new tag:";
                name = inputDialog(instruction); // will return null if user cancels
                if (name != null)
                    valid = validInput(name) && !parent.hasChild(name);
                else
                    valid = false;
            } while (name != null && !valid);

            if (valid)
            {
                // Create a TagNode child for the new tag and add it to the database
                TagNode child = new TagNode(parent, name);
                parent.getChildren().add(child);
                ReadWriteManager.addTag(child);

                // Add a new CheckBoxTreeItem to the CheckTreeView for the new tag
                CheckBoxTreeItem<String> nodeItem = new CheckBoxTreeItem<>(child.getTag());
                if (selectedItem != null)
                    selectedItem.getChildren().add(nodeItem);
                else
                    TREE_VIEW.getRoot().getChildren().add(nodeItem);
            }
        }

        private String inputDialog(String instruction)
        {
            // Open a dialog that asks the user to input a name for this new tag
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Create Tag");
            nameDialog.setHeaderText(instruction);
            nameDialog.setGraphic(null);
            nameDialog.showAndWait();
            return nameDialog.getResult();
        }

        private boolean validInput(String input)
        {
            for (char c : input.toCharArray())
            {
                if (c == '/' || c == '\\' || c == '"' || c == '\'')
                    return false;
            }
            return true;
        }
    }
}