package com.example.tagger;

import com.example.tagger.controllers.TagEditorController;
import com.example.tagger.controllers.TagSelectorController;
import com.example.tagger.miscellaneous.ExtensionFilter;
import com.example.tagger.miscellaneous.TagNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;

public class IOManager
{
    public static boolean validInput(String input)
    {
        for (char c : input.toCharArray())
        {
            if (c == '/' || c == '\\' || c == '"' || c == '\'')
                return false;
        }
        return true;
    }

    public static boolean confirmAction(String title, String header, String description)
    {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(title);
        confirmation.setHeaderText(header);
        confirmation.setContentText(description);
        confirmation.setGraphic(null);
        confirmation.getDialogPane().setMaxWidth(400);
        confirmation.showAndWait();
        return (confirmation.getResult() == ButtonType.OK);
    }

    public static void showError(String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.showAndWait();
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

    // Open a window to edit the provided TagNode
    public static void editTag(Window owner, TagNode tag)
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

    /* Return a list of all files in the provided directory that pass the filter (needs a compatible extension),
     * or return null if the path is not a valid directory or does not permit its files to be read */
    public static File[] getFilesInDir(final String PATH)
    {
        File directory = new File(PATH);
        if (directory.isDirectory())
        {
            try
            {
                return directory.listFiles(new ExtensionFilter());
            }
            catch (SecurityException exception)
            {
                System.out.printf("Database.getFilesInDir: Does not have permission to read files in directory \"%s\"", PATH);
                return null;
            }
        }
        else
        {
            System.out.printf("Database.getFilesInDir: Path \"%s\" does not lead to a valid directory", PATH);
            return null;
        }
    }

    public static boolean renameFile(String path, String oldName, String newName)
    {
        // Attempt to rename the actual file's name
        String oldPath = String.format("%s/Storage/%s", path, oldName);
        String newPath = String.format("%s/Storage/%s", path, newName);

        File file = new File(oldPath);
        if (file.renameTo(new File(newPath)))
        {
            if (Database.renameFileInDatabase(path, oldName, newName))
                return true;
            else
            {
                // If unable to rename the file in the database, attempt to revert the actual file's name for consistency
                if (!file.renameTo(new File(oldPath)))
                    System.out.println("IOManager.renameFile: Unable to revert filename");
                return false;
            }
        }
        else
        {
            System.out.println("IOManager.renameFile: Unable to rename file");
            return false;
        }
    }

    public static void deleteFile(String path, String fileName)
    {
        // Delete the actual file from the managed directory
        File file = new File(String.format("%s/Storage/%s", path, fileName));
        if (!file.delete())
            System.out.println("IOManager.deleteFile: Unable to delete file");

        // Delete the file's information from the database
        Database.deleteFileFromDatabase(path, fileName);
    }
}
