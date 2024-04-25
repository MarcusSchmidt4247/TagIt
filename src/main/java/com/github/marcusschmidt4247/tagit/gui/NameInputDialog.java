/* TagIt
 * NameInputDialog.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.gui;

import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.ExtensionFilter;
import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
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
                        WindowManager.showError("Unsupported file type");
                    else
                    {
                        if (parent != null)
                            WindowManager.showError(String.format("\"%s\" already has a child tag \"%s\"", parent.getTag(), name));
                        else
                        {
                            System.out.println("NameInputDialog.showAndLoop: parent == null");
                            WindowManager.showError("Unable to create tag");
                        }
                    }
                }
                else
                    WindowManager.showError("Name cannot contain slashes or quotes");
            }
        } while (name != null && !valid);
        return valid;
    }
}
