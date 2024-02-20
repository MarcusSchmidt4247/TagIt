package com.example.tagger.controllers;

import com.example.tagger.Database;
import com.example.tagger.IOManager;
import com.example.tagger.miscellaneous.TagNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Vector;

public class TagEditorController
{
    @FXML
    TextField nameField;
    @FXML
    Label parentageLabel;

    private TagNode root = null;
    private TagNode tag = null;
    private TagNode currentParent;

    public void setTagNodes(TagNode root, TagNode tag)
    {
        if (this.root == null)
        {
            this.root = root;
            this.tag = tag;
            nameField.setText(tag.getTag());

            currentParent = tag.getParent();
            setParentLabel();
        }
    }

    public void onSelect()
    {
        TagNode selection = IOManager.selectTag(nameField.getScene().getWindow(), root, tag);
        if (selection != null)
        {
            currentParent = selection;
            setParentLabel();
        }
    }

    public void onSaveButton()
    {
        if (nameField.getText().isBlank())
        {
            errorDialog("Name cannot be blank");
            return;
        }
        else if (!IOManager.validInput(nameField.getText()))
        {
            errorDialog("Name cannot contain slashes or quotes");
            return;
        }

        // If the name field has been edited, update the name of the node (it has already been checked for validity)
        if (!tag.getTag().equals(nameField.getText()))
        {
            tag.setTag(nameField.getText());
            Database.renameTag(tag);
        }

        // If a new parent has been selected, update the tag's parent
        if (!tag.getParent().equals(currentParent))
            tag.changeParent(currentParent);

        ((Stage) nameField.getScene().getWindow()).close();
    }

    public void onCancelButton()
    {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    public void onDeleteButton()
    {
        String description = "All files tagged with this item will lose the tag, and all child tags will be deleted too. This action cannot be reversed.";
        if (IOManager.confirmAction("Delete Tag", "Are you sure you want to delete this tag?", description))
        {
            boolean delete = true;
            boolean switchTag = false;

            // Get the list of files that are only tagged with the tag that is about to be deleted
            Vector<String> orphanedFiles = Database.getUniqueFiles(tag);
            if (!orphanedFiles.isEmpty())
            {
                Alert warning = new Alert(Alert.AlertType.WARNING);
                warning.setTitle("Orphaned Files");
                warning.setHeaderText(String.format("This is the only tag for %d files.", orphanedFiles.size()));
                warning.setContentText("These files will be inaccessible without at least one tag. They must either be deleted or moved to another tag.");
                // Get rid of the default button and add three custom  buttons with the appropriate choices
                warning.getButtonTypes().removeFirst();
                warning.getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.RIGHT));
                ButtonType deleteButton = new ButtonType("Delete Files", ButtonBar.ButtonData.RIGHT);
                warning.getButtonTypes().add(deleteButton);
                ButtonType newTagButton = new ButtonType("Select Tag", ButtonBar.ButtonData.RIGHT);
                warning.getButtonTypes().add(newTagButton);
                // Set the button to select a new tag for these files as the default button
                ((Button) warning.getDialogPane().lookupButton(newTagButton)).setDefaultButton(true);
                // Show the dialog and record the user's choice
                warning.showAndWait();
                delete = (warning.getResult() == deleteButton);
                switchTag = (warning.getResult() == newTagButton);
            }

            if (delete)
            {
                orphanedFiles.forEach(orphan -> IOManager.deleteFile(tag.getRootPath(), orphan));
                tag.delete();
                ((Stage) nameField.getScene().getWindow()).close();
            }
            else if (switchTag)
            {
                // If the user chose to move the files to a new tag, open a window to select a tag
                TagNode selection = IOManager.selectTag(nameField.getScene().getWindow(), root, tag);
                if (selection != null)
                {
                    // Change the tag for these files to the new selection
                    for (String orphan : orphanedFiles)
                    {
                        Database.deleteFileTag(orphan, tag);
                        Database.addFileTag(orphan, selection);
                    }

                    // And then delete the old tag
                    tag.delete();
                    ((Stage) nameField.getScene().getWindow()).close();
                }
            }
        }
    }

    private void errorDialog(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    // Update the label to show this tag's parentage
    private void setParentLabel()
    {
        String parentage = "";
        if (currentParent != null && currentParent.getTagPath() != null)
            parentage = currentParent.getTagPath();
        parentageLabel.setText(parentage);
    }
}
