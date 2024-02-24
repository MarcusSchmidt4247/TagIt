package com.example.tagger.gui;

import com.example.tagger.miscellaneous.ExtensionFilter;
import com.example.tagger.IOManager;
import com.example.tagger.miscellaneous.TagNode;
import javafx.scene.control.TextInputDialog;

public class NameInputDialog extends TextInputDialog
{
    public enum Type { FILE, TAG }
    private Type type;

    private String name = null;
    public String getName() { return name; }

    private TagNode parent;

    public NameInputDialog(String defaultFileName)
    {
        this("file", defaultFileName);
        type = Type.FILE;
    }

    public NameInputDialog(String defaultTagName, TagNode parent)
    {
        this("tag", defaultTagName);
        type = Type.TAG;
        this.parent = parent;
    }

    private NameInputDialog(String typeName, String defaultName)
    {
        super(defaultName);
        setHeaderText(String.format("Enter a new %s name:", typeName));
        setTitle("");
        setGraphic(null);
        getDialogPane().setMinWidth(300);
    }

    // Show the dialog repeatedly until the user cancels or enters a valid file name, and then return whether a valid name was entered
    public boolean showAndLoop()
    {
        boolean valid;
        do
        {
            valid = false;
            showAndWait();
            name = getResult(); // will return null if user cancels dialog
            if (name != null)
            {
                if (IOManager.validInput(name))
                {
                    if ((type == Type.FILE && ExtensionFilter.validExtension(name)) ||
                        (type == Type.TAG && parent != null && !parent.hasChild(name)))
                        valid = true;
                    else if (type == Type.FILE)
                        IOManager.showError("Unsupported file type");
                    else
                    {
                        if (parent != null)
                            IOManager.showError(String.format("\"%s\" already has a child tag \"%s\"", parent.getTag(), name));
                        else
                        {
                            System.out.println("NameInputDialog.showAndLoop: parent == null");
                            IOManager.showError("Unable to create tag");
                        }
                    }
                }
                else
                    IOManager.showError("Name cannot contain slashes or quotes");
            }
        } while (name != null && !valid);
        return valid;
    }
}
