/* TagIt
 * NameInputDialog.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.gui;

import com.github.marcusschmidt4247.tagit.WindowManager;
import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.FileTypes;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class NameInputDialog extends TextInputDialog
{
    public enum Type { FILE, TAG }
    private final Type type;

    private String name = null;
    /**
     * Gets the most recent name entered into this dialog.
     * <p/>
     * If called after <code>showAndLoop()</code> returns <code>true</code>, <code>name</code>
     * is guaranteed to be valid. Otherwise, <code>name</code> might be unsafe to use.
     * @return the user's most recent input; <code>null</code> if dialog was cancelled
     */
    public String getName() { return name; }

    private TagNode parent;

    private boolean isRoot = false;
    /**
     * Checks if the name provided by the user is for a root tag.
     * @return <code>true</code> if naming a root tag; <code>false</code> otherwise
     */
    public boolean getIsRoot() { return isRoot; }

    /**
     * Class constructor to name a file.
     * @param defaultName the text that pre-fills the name field
     */
    public NameInputDialog(String defaultName)
    {
        super(defaultName);
        type = Type.FILE;
        setHeaderText("Enter a new file name:");
        setTitle("");
        setGraphic(null);
        getDialogPane().setMinWidth(300);
    }

    /**
     * Class constructor to name a tag.
     * @param defaultName the text that pre-fills the name field
     * @param parent the <code>TagNode</code> that will parent this tag unless it's marked as root
     *               (cannot be <code>null</code>)
     */
    public NameInputDialog(String defaultName, TagNode parent)
    {
        type = Type.TAG;
        this.parent = parent;

        /* Override the default dialog pane to add a CheckBox
         * Reference: https://stackoverflow.com/questions/36949595 */
        setDialogPane(new DialogPane()
        {
            @Override
            protected Node createDetailsButton()
            {
                CheckBox checkBox = new CheckBox();
                checkBox.setText("Root Tag");
                checkBox.setOnAction(event -> isRoot = checkBox.isSelected());
                return checkBox;
            }
        });
        getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        getDialogPane().setExpandableContent(new Group());
        getDialogPane().setExpanded(true);

        /* Reestablish TextInputDialog behaviors
         * Reference: https://github.com/ojdkbuild/lookaside_openjfx/blob/master/modules/controls/src/main/java/javafx/scene/control/TextInputDialog.java */
        TextField textField = new TextField(defaultName);
        GridPane.setHgrow(textField, Priority.ALWAYS);
        GridPane.setFillWidth(textField, true);
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setAlignment(Pos.CENTER_LEFT);
        gridPane.add(textField, 0, 0);
        getDialogPane().setContent(gridPane);
        Platform.runLater(textField::requestFocus);
        setResultConverter((dialogButton) -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? textField.getText() : null;
        });

        // Finish configuring the dialog
        setHeaderText("Enter a new tag name:");
        setTitle("");
        getDialogPane().setGraphic(null);
        getDialogPane().setMinWidth(300);
    }

    /**
     * Shows this dialog until the user cancels or enters a valid name.
     * @return <code>true</code> if a valid name was entered; <code>false</code> otherwise
     */
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
                    if ((type == Type.FILE && FileTypes.isSupported(name)) ||
                        (type == Type.TAG && parent != null && ((isRoot && !parent.getRoot().hasChild(name)) || (!isRoot && !parent.hasChild(name)))))
                        valid = true;
                    else if (type == Type.FILE)
                        WindowManager.showError("Unsupported file type");
                    else
                    {
                        if (parent != null)
                            WindowManager.showError(String.format("\"%s\" already has a child tag \"%s\"", isRoot ? parent.getRoot().getTag() : parent.getTag(), name));
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
