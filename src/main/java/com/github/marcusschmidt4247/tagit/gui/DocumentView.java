/* TagIt
 * DocumentView.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.*;

public class DocumentView extends VBox
{
    private static final int MAX_CHAR_PER_PAGE = 3000;

    private enum Type { TXT, UNSUPPORTED }

    private final ScrollPane contentPane;
    private final Label content;
    private final HBox pageControlsLayout;
    private final Button prevPageButton;
    private final Button nextPageButton;

    private FileReader fileReader = null;
    private File file = null;
    private Type type = Type.UNSUPPORTED;
    private int nextPage = 0;
    private int lastPage = -1;

    public DocumentView()
    {
        super();

        // Add the scrollable content pane
        contentPane = new ScrollPane();
        contentPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        getChildren().add(contentPane);
        setVgrow(contentPane, Priority.ALWAYS);

        // Add a label to the scrollable pane that will display the content
        content = new Label("This document is empty");
        content.prefWidthProperty().bind(contentPane.widthProperty());
        content.setPadding(new Insets(5, 10, 0, 10));
        content.setWrapText(true);
        contentPane.setContent(content);

        // Create an HBox with controls for changing the document page
        pageControlsLayout = new HBox();
        pageControlsLayout.setAlignment(Pos.CENTER);
        pageControlsLayout.setSpacing(5);
        pageControlsLayout.setPadding(new Insets(5, 0, 5, 0));
        // Button to go back a page
        prevPageButton = new Button();
        prevPageButton.setGraphic(new FontIcon("bx-left-arrow"));
        prevPageButton.setOnAction(actionEvent -> onPrevPage());
        pageControlsLayout.getChildren().add(prevPageButton);
        // Button to go forward a page
        nextPageButton = new Button();
        nextPageButton.setGraphic(new FontIcon("bx-right-arrow"));
        nextPageButton.setOnAction(actionEvent -> onNextPage());
        pageControlsLayout.getChildren().add(nextPageButton);
        getChildren().add(pageControlsLayout);
    }

    // Load a new file into the DocumentView
    public void load(File file)
    {
        // Close the previous file if not already
        close();

        this.file = file;
        pageControlsLayout.setVisible(true);

        // Determine the file type
        int dotIndex = file.getName().lastIndexOf('.');
        if (dotIndex != -1)
        {
            String extension = file.getName().substring(dotIndex).toLowerCase();
            if (extension.equals(".txt"))
                type = Type.TXT;
            else
                System.out.printf("DocumentView.load: Unrecognized file extension \"%s\"\n", extension);
        }
        else
            System.out.println("DocumentView.load: No file extension");

        // Show the first page
        if (type != Type.UNSUPPORTED)
            viewPage(nextPage);
    }

    // When the previous file is no longer needed, close the file reader and set file attributes to the defaults
    public void close()
    {
        if (fileReader != null)
        {
            try
            {
                fileReader.close();
                fileReader = null;
            }
            catch (IOException exception)
            {
                System.out.println("DocumentView.close: Failed to close fileReader");
            }
        }

        this.file = null;
        type = Type.UNSUPPORTED;
        nextPage = 0;
        lastPage = -1;
    }

    // Move to the document's next page
    private void onNextPage()
    {
        if (file != null && (lastPage == -1 || nextPage <= lastPage))
            viewPage(nextPage);
    }

    // Move to the document's previous page
    private void onPrevPage()
    {
        if (file != null && nextPage > 1)
            viewPage(nextPage - 2);
    }

    // Show the page in the document with the provided index
    private void viewPage(int index)
    {
        if (type == Type.UNSUPPORTED || (fileReader == null && !openFileReader()))
            return;

        // Reset the scrollbar to the top of the new page
        contentPane.setVvalue(0);

        // Unless reading the next page in the document, move the file reader to its new location
        if (index != nextPage)
            moveToPage(index);

        // Parse the file for this page of content and display it with the content label
        String pageContents = readNextPage();
        if (pageContents != null)
            content.setText(pageContents);

        // Disable the button to go to the next page only when on the last page
        if (lastPage != -1 && nextPage > lastPage)
            nextPageButton.setDisable(true);
        else if (nextPageButton.isDisabled())
            nextPageButton.setDisable(false);

        // Disable the button to go to the previous page only when on the first page
        if (nextPage == 1)
            prevPageButton.setDisable(true);
        else if (prevPageButton.isDisabled())
            prevPageButton.setDisable(false);
    }

    // Move the file reader to the start of the given page in the file
    private void moveToPage(int targetPage)
    {
        if (fileReader == null)
        {
            System.out.println("DocumentView.moveToPage: fileReader is null");
            return;
        }

        try
        {
            // Unless reading a future page, reset the file reader to the beginning of the file
            if (targetPage < nextPage)
                resetFileReader();

            // Move forward one page at a time until the start of the target page has been reached
            while (nextPage < targetPage)
            {
                long skipped = fileReader.skip(MAX_CHAR_PER_PAGE);
                if (skipped == MAX_CHAR_PER_PAGE)
                    nextPage++;
                else
                {
                    // If less than a full page could be skipped, then either this or the previously read page must be the last one
                    if (skipped == 0)
                        lastPage = nextPage - 1;
                    else
                        lastPage = nextPage;

                    // In order to go back to the start of the last full page, reset again and call another move to the last page
                    resetFileReader();
                    moveToPage(lastPage);
                    return;
                }
            }
        }
        catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }

    // Parse raw file data at the file reader's current location into the next full page of text
    private String readNextPage()
    {
        if (fileReader == null)
        {
            System.out.println("DocumentView.parseFile: fileReader is null");
            return null;
        }

        try
        {
            // Attempt to read the maximum number of characters for a page
            char[] buffer = new char[MAX_CHAR_PER_PAGE];
            int length = fileReader.read(buffer, 0, MAX_CHAR_PER_PAGE);
            if (length != -1)
            {
                // If less than a full page was able to be read, mark that this page must be the last one
                if (length < MAX_CHAR_PER_PAGE)
                {
                    lastPage = nextPage;
                    // If the document is a single page, hide the page controls
                    if (lastPage == 0)
                        pageControlsLayout.setVisible(false);
                }

                nextPage++;
                return new String(buffer, 0, length);
            }
            else
            {
                // If there was nothing to be read, the previous page must be the last one
                lastPage = nextPage - 1;
                System.out.println("DocumentView.parseFile: Already reached EOF");
                return null;
            }
        }
        catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }

    private boolean openFileReader()
    {
        try
        {
            fileReader = new FileReader(file);
            return true;
        }
        catch (FileNotFoundException exception)
        {
            System.out.printf("DocumentView.openFileReader: %s\n", exception.toString());
            return false;
        }
    }

    private void resetFileReader() throws IOException
    {
        if (fileReader != null)
        {
            nextPage = 0;
            fileReader.close();
            openFileReader();
        }
    }
}
