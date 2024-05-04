/* TagIt
 * MultiMediaView.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.gui;

import com.github.marcusschmidt4247.tagit.miscellaneous.FileTypes;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MultiMediaView extends StackPane
{
    private Label errorLabel;
    private DocumentView documentView;
    private ImageView imageView;
    private MediaControlView mediaControlView;

    // Must be called before MultiMediaView can be used
    public void init(boolean bordered)
    {
        // Prepare to bind the MultiMediaView components to fill its dimensions
        ObservableDoubleValue heightBinding = heightProperty();
        ObservableDoubleValue widthBinding = widthProperty();
        if (bordered)
        {
            /* If MultiMediaView is styled to have a border, its components must be bound slightly smaller than its dimensions
             * because a bordered MultiMediaView will always grow to be slightly bigger than its contents, and so binding them
             * directly will lead to infinite growth. */
            heightBinding = new OffsetDoubleBinding(heightProperty(), 10.0);
            widthBinding = new OffsetDoubleBinding(widthProperty(), 10.0);
        }

        errorLabel = new Label();
        getChildren().add(errorLabel);

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setVisible(false);
        imageView.fitHeightProperty().bind(heightBinding);
        imageView.fitWidthProperty().bind(widthBinding);
        getChildren().add(imageView);

        mediaControlView = new MediaControlView();
        mediaControlView.setVisible(false);
        mediaControlView.fitHeightProperty().bind(heightBinding);
        mediaControlView.fitWidthProperty().bind(widthBinding);
        getChildren().add(mediaControlView);
        mediaControlView.init(); // must be initiated *after* being added to its parent

        documentView = new DocumentView();
        documentView.setVisible(false);
        documentView.prefHeightProperty().bind(heightBinding);
        documentView.prefWidthProperty().bind(widthBinding);
        getChildren().add(documentView);
    }

    // Load and display the provided file with the appropriate view depending on its file type
    public void load(File file)
    {
        if (errorLabel == null)
        {
            System.out.println("MultiMediaView.load: init() must be called first");
            return;
        }

        FileTypes.Type fileType = FileTypes.Type.UNSUPPORTED;
        if (file == null)
            errorLabel.setText("No files");
        else
        {
            fileType = FileTypes.getType(file.getName());
            if (fileType == FileTypes.Type.TEXT)
                documentView.load(file);
            else if (fileType == FileTypes.Type.IMAGE)
            {
                try (FileInputStream input = new FileInputStream(file))
                {
                    Image image = new Image(input);
                    imageView.setImage(image);
                }
                catch (IOException e)
                {
                    errorLabel.setText(String.format("Unable to load image file \"%s\"", file.getName()));
                    fileType = FileTypes.Type.UNSUPPORTED;
                }
            }
            else if (fileType == FileTypes.Type.VIDEO)
                mediaControlView.load(file);
            else
                errorLabel.setText(String.format("\"%s\" is an unsupported file type", file.getName()));
        }

        setVisibility(fileType);
    }

    // Set the visibility for each component of the MultiMediaView according to the type of file it is currently displaying
    private void setVisibility(FileTypes.Type fileType)
    {
        if (errorLabel.isVisible() != (fileType == FileTypes.Type.UNSUPPORTED))
            errorLabel.setVisible(fileType == FileTypes.Type.UNSUPPORTED);

        if (documentView.isVisible() != (fileType == FileTypes.Type.TEXT))
        {
            documentView.setVisible(fileType == FileTypes.Type.TEXT);
            if (!documentView.isVisible())
                documentView.close();
        }

        if (imageView.isVisible() != (fileType == FileTypes.Type.IMAGE))
            imageView.setVisible(fileType == FileTypes.Type.IMAGE);

        if (mediaControlView.isVisible() != (fileType == FileTypes.Type.VIDEO))
            mediaControlView.setVisibility(fileType == FileTypes.Type.VIDEO);
    }

    // A class that binds two Double properties together with an offset
    private static class OffsetDoubleBinding extends DoubleBinding
    {
        private final ObservableDoubleValue PROPERTY;
        private final double OFFSET;
        private double prevResult;

        public OffsetDoubleBinding(ObservableDoubleValue property, final Double OFFSET)
        {
            PROPERTY = property;
            bind(PROPERTY);
            this.OFFSET = OFFSET;
        }

        @Override
        protected double computeValue()
        {
            /* If the bound value is closer to this value than a quarter of the offset, then it must be decreasing
             * quickly and should be given more room to shrink per frame */
            double nextOffset = OFFSET;
            if (PROPERTY.doubleValue() < prevResult + (OFFSET / 4))
                nextOffset *= 3;
            return (prevResult = PROPERTY.doubleValue() - nextOffset);
        }
    }
}
