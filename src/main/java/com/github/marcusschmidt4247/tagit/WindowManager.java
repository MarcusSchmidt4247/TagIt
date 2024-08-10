/* TagIt
 * WindowManager.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit;

import com.github.marcusschmidt4247.tagit.controllers.*;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class WindowManager
{
    private static Stage managedFoldersStage = null;

    public static boolean customConfirmationDialog(String title, String header, String description, String posButtonText)
    {
        Alert confirmation = new Alert(Alert.AlertType.NONE);
        confirmation.getDialogPane().setMaxWidth(400);
        confirmation.setTitle(title);
        confirmation.setHeaderText(header);
        confirmation.setContentText(description);
        confirmation.getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
        ButtonType confirmButtonType = new ButtonType(posButtonText, ButtonBar.ButtonData.OK_DONE);
        confirmation.getButtonTypes().add(confirmButtonType);
        confirmation.showAndWait();
        return confirmation.getResult().equals(confirmButtonType);
    }

    public static boolean confirmationDialog(String title, String header, String description) { return customConfirmationDialog(title, header, description, "OK"); }

    public static void showError(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    // Open a new main window for the provided managed folder
    public static void openFolder(ManagedFolder folder) { openFolder(folder, new Stage()); }

    // Open the provided managed folder onto the given stage
    public static void openFolder(ManagedFolder folder, Stage stage)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("tagger-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 850, 600);
            TaggerController taggerController = fxmlLoader.getController();
            taggerController.setFolder(folder);
            scene.addEventFilter(KeyEvent.KEY_PRESSED, taggerController::keyEventHandler);

            stage.titleProperty().bind(folder.nameProperty());
            stage.setScene(scene);
            stage.show();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Create a window to view, create, delete, and open the editor for ManagedFolder objects
    public static void openFolderManager()
    {
        if (managedFoldersStage == null)
        {
            try
            {
                FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("managed-folders-view.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                managedFoldersStage = new Stage();
                managedFoldersStage.setTitle("Managed Folders");
                managedFoldersStage.setMinWidth(520);
                managedFoldersStage.setMinHeight(400);
                managedFoldersStage.setScene(scene);
                managedFoldersStage.showAndWait();
                managedFoldersStage = null;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            managedFoldersStage.toFront();
    }

    // Create a window to view all ManagedFolder objects and select one to replace the current main window
    public static void switchFolder(Window currentWindow, ManagedFolder currentFolder)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("managed-folders-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            ManagedFoldersController controller = fxmlLoader.getController();
            controller.setSelectionOnly(currentFolder);
            Stage stage = new Stage();
            stage.initOwner(currentWindow);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Managed Folders");
            stage.setMinWidth(520);
            stage.setMinHeight(400);
            stage.setScene(scene);
            stage.showAndWait();

            ManagedFolder selection = controller.getSelection();
            if (selection != null)
            {
                openFolder(selection);
                ((Stage) currentWindow).close();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Create a window to edit a ManagedFolder object
    public static void openFolderEditor(Window owner, ManagedFolder folder)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("managed-folder-editor-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            ((ManagedFolderEditorController) fxmlLoader.getController()).setFolders(folder);
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.setMinWidth(400);
            stage.setResizable(false);
            stage.setTitle((folder != null) ? "Edit Folder" : "Create Folder");
            stage.setScene(scene);
            stage.showAndWait();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Create a window to select a tag from the provided TagNode tree (preselectedTag can be null)
    public static TagNode selectTag(TagNode treeRoot, TagNode preselectedTag)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("tag-selector-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            TagSelectorController controller = fxmlLoader.getController();
            controller.setTagNodes(treeRoot, preselectedTag);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Tag Selector");
            stage.setMinWidth(200);
            stage.setMinHeight(200);
            stage.setScene(scene);
            stage.showAndWait();
            return controller.getSelection();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Create a window to edit the provided TagNode
    public static void openTagEditor(Window owner, TagNode tag)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(IOManager.class.getResource("tag-editor-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            ((TagEditorController) fxmlLoader.getController()).setTag(tag);
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.setResizable(false);
            stage.setTitle("Edit Tag");
            stage.setScene(scene);
            stage.showAndWait();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
