package com.example.tagger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class IOManager
{
    // Create a window to select a tag from the provided TagNode tree
    public static TagNode selectTag(Window owner, TagNode treeRoot, TagNode preselectedTag)
    {
        try
        {
            FXMLLoader fxmlLoader = new FXMLLoader(TaggerApplication.class.getResource("tag-selector-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            TagSelectorController controller = fxmlLoader.getController();
            controller.setTagNodes(treeRoot, preselectedTag);
            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
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
}
