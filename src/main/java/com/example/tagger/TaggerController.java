package com.example.tagger;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import org.controlsfx.control.CheckTreeView;

import java.io.*;
import java.util.Vector;

public class TaggerController
{
    @FXML
    private CheckTreeView<String> tagTreeView;
    @FXML
    private AnchorPane contentPane;
    @FXML
    private Label errorLabel;
    @FXML
    private ImageView imageView;

    private final TaggerModel TAGGER_MODEL = new TaggerModel("placeholder");

    public void initialize()
    {
        // Set the image view to scale with the window
        imageView.fitWidthProperty().bind(contentPane.widthProperty());
        imageView.fitHeightProperty().bind(contentPane.heightProperty());

        // Read the tags into the program and populate the check tree view with them
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(String.format("%s/tags.txt", TAGGER_MODEL.getPath()))))
        {
            String line = bufferedReader.readLine();
            CheckBoxTreeItem<String> parent = (CheckBoxTreeItem<String>) tagTreeView.getRoot();
            int treeDepth = 0;
            while (line != null)
            {
                // Regex of expected line format: -*>.+
                int newDepth = line.indexOf('>');
                String tag = line.substring(newDepth + 1);
                CheckBoxTreeItem<String> node = new CheckBoxTreeItem<>(tag);
                node.setIndependent(true);

                if (newDepth > treeDepth)
                {
                    parent = (CheckBoxTreeItem<String>) parent.getChildren().getLast();
                    treeDepth++;
                }
                else
                {
                    while (newDepth < treeDepth)
                    {
                        parent = (CheckBoxTreeItem<String>) parent.getParent();
                        treeDepth--;
                    }
                }

                parent.getChildren().add(node);
                line = bufferedReader.readLine();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // Docs: https://controlsfx.github.io/javadoc/11.1.0/org.controlsfx.controls/org/controlsfx/control/CheckTreeView.html
        //       https://docs.oracle.com/javase/8/javafx/api/javafx/collections/ListChangeListener.Change.html
        tagTreeView.getCheckModel().getCheckedItems().addListener(new ListChangeListener<TreeItem<String>>()
        {
            @Override
            public void onChanged(Change<? extends TreeItem<String>> change)
            {
                // Get all active tags
                Vector<String> activeTags = new Vector<>();
                for (TreeItem<String> treeItem : tagTreeView.getCheckModel().getCheckedItems())
                    activeTags.add(treeItem.getValue());

                // Gather the names of the files with those tags
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(String.format("%s/files.txt", TAGGER_MODEL.getPath()))))
                {
                    Vector<String> selectedFiles = new Vector<>();
                    String line = bufferedReader.readLine();
                    while (line != null)
                    {
                        // Regex of expected line format: .+:(.+,)*.+
                        int delimiterIndex = line.indexOf(':');
                        String[] fileTags = line.substring(delimiterIndex + 1).split(",");
                        for (String tag : fileTags)
                        {
                            if (activeTags.contains(tag))
                            {
                                selectedFiles.add(line.substring(0, delimiterIndex));
                                break;
                            }
                        }
                        line = bufferedReader.readLine();
                    }
                    TAGGER_MODEL.setFiles(selectedFiles);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

                refreshContentPane(TAGGER_MODEL.firstFile());
            }
        });
    }

    public void keyEventHandler(KeyEvent event)
    {
        event.consume();
        if (event.getCode() == KeyCode.LEFT)
            refreshContentPane(TAGGER_MODEL.prevFile());
        else if (event.getCode() == KeyCode.RIGHT)
            refreshContentPane(TAGGER_MODEL.nextFile());
    }

    private void refreshContentPane(String fileName)
    {
        if (fileName != null)
        {
            String filePath = String.format("%s/Storage/%s", TAGGER_MODEL.getPath(), fileName);
            try (FileInputStream input = new FileInputStream(filePath))
            {
                Image image = new Image(input);
                imageView.setImage(image);

                if (!imageView.isVisible())
                    imageView.setVisible(true);
            }
            catch (IOException e)
            {
                errorLabel.setText(String.format("Unable to load file \"%s\"", fileName));
                imageView.setVisible(false);
                throw new RuntimeException(e);
            }
        }
        else
        {
            errorLabel.setText("No files selected");
            imageView.setVisible(false);
        }
    }
}