package com.example.tagger;

import javafx.scene.control.TextInputDialog;

public class FileNameDialog extends TextInputDialog
{
    private String name = null;
    public String getName() { return name; }

    public FileNameDialog(String defaultInput)
    {
        super(defaultInput);
        setHeaderText("Enter a new file name:");
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
            if (name != null)
                setHeaderText("Name cannot contain slashes or quotes and must\nhave a valid extension.\n\nEnter a new file name:");
            showAndWait();
            name = getResult(); // will return null if user cancels dialog
            valid = (name != null && ReadWriteManager.validInput(name) && ExtensionFilter.validExtension(name));
        } while (name != null && !valid);
        return valid;
    }
}
