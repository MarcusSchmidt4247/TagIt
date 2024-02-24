package com.example.tagger.controllers;

import com.example.tagger.Database;
import com.example.tagger.IOManager;
import com.example.tagger.miscellaneous.TagNode;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class TagEditorController
{
    @FXML
    TextField nameField;
    @FXML
    Label parentageLabel;

    private TagNode tag = null;
    private TagNode currentParent;

    public void setTag(TagNode tag)
    {
        if (this.tag == null)
        {
            this.tag = tag;
            nameField.setText(tag.getTag());

            currentParent = tag.getParent();
            setParentLabel();
        }
    }

    public void onSelect()
    {
        TagNode selection = IOManager.selectTag(tag.getRoot(), tag);
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
            IOManager.showError("Name cannot be blank");
            return;
        }
        else if (!IOManager.validInput(nameField.getText()))
        {
            IOManager.showError("Name cannot contain slashes or quotes");
            return;
        }
        else if (currentParent.hasChild(nameField.getText()))
        {
            IOManager.showError(String.format("\"%s\" already has a child tag \"%s\"", currentParent.getTag(), nameField.getText()));
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
            if (tag.delete())
                ((Stage) nameField.getScene().getWindow()).close();
        }
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
