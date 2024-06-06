/* TagIt
 * DocumentView.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.gui;

import com.github.marcusschmidt4247.tagit.parsers.DocxParser;
import com.github.marcusschmidt4247.tagit.parsers.Parser;
import com.github.marcusschmidt4247.tagit.parsers.ParserResults;
import com.github.marcusschmidt4247.tagit.parsers.TxtParser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.*;

public class DocumentView extends VBox
{
    private static final Font defaultFont = Font.font("Verdana", 14);
    private static final int MAX_CHAR_PER_PAGE = 3000;

    private enum Type { TXT, DOCX, UNSUPPORTED }

    private final ScrollPane scrollPane;
    private final VBox contentPane;
    private final HBox pageControlsLayout;
    private final Button prevPageButton;
    private final Button nextPageButton;

    private Parser parser = null;
    private File file = null;
    private Type type = Type.UNSUPPORTED;
    private int lastPage = -1;

    public DocumentView()
    {
        super();

        // Add the scrollable pane
        scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToWidth(true);
        getChildren().add(scrollPane);
        setVgrow(scrollPane, Priority.ALWAYS);

        // Add the pane where TextFlows containing the document's content will be added
        contentPane = new VBox();
        contentPane.setPadding(new Insets(5, 10, 0, 10));
        scrollPane.setContent(contentPane);

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
            else if (extension.equals(".docx"))
                type = Type.DOCX;
            else
                System.out.printf("DocumentView.load: Unrecognized file extension \"%s\"\n", extension);
        }
        else
            System.out.println("DocumentView.load: No file extension");

        // Show the first page
        if (type != Type.UNSUPPORTED)
            viewPage();
    }

    // When the previous file is no longer needed, close the file reader and set file attributes to the defaults
    public void close()
    {
        if (parser != null)
        {
            parser.close();
            parser = null;
        }

        this.file = null;
        type = Type.UNSUPPORTED;
        lastPage = -1;
    }

    // Move to the document's next page
    private void onNextPage()
    {
        if (file != null && (lastPage == -1 || parser.getNextPage() <= lastPage))
            viewPage();
    }

    // Move to the document's previous page
    private void onPrevPage()
    {
        if (file != null && parser.getNextPage() > 1)
            viewPage(parser.getNextPage() - 2);
    }

    // Show the next page in the document
    private void viewPage() { viewPage(-1); }

    // Show the page in the document with the provided index (-1 defaults to the next page)
    private void viewPage(int index)
    {
        if (type == Type.UNSUPPORTED || !accessFile())
            return;

        // Reset the scrollbar to the top of the new page
        scrollPane.setVvalue(0);

        // Unless reading the next page in the document, move the file reader to its new location
        if (index == -1 || parser.setNextPage(index, MAX_CHAR_PER_PAGE))
        {
            ParserResults results = parser.readNextPage(defaultFont, MAX_CHAR_PER_PAGE);
            if (results == null)
                lastPage = parser.getNextPage() - 1;
            else
            {
                if (results.isEndOfFile())
                {
                    lastPage = parser.getNextPage() - 1;
                    // If the document is a single page, hide the page controls
                    if (lastPage == 0)
                        pageControlsLayout.setVisible(false);
                }
                contentPane.getChildren().clear();
                results.getNodes().forEach(node ->
                {
                    node.prefWidthProperty().bind(contentPane.widthProperty());
                    contentPane.getChildren().add(node);
                });
            }

            // Disable the button to go to the next page only when on the last page
            if (lastPage != -1 && parser.getNextPage() > lastPage)
                nextPageButton.setDisable(true);
            else if (nextPageButton.isDisabled())
                nextPageButton.setDisable(false);

            // Disable the button to go to the previous page only when on the first page
            if (parser.getNextPage() == 1)
                prevPageButton.setDisable(true);
            else if (prevPageButton.isDisabled())
                prevPageButton.setDisable(false);
        }
        else
            System.out.printf("DocumentView.viewPage: Failed to move to page %d\n", index);
    }

    private boolean accessFile()
    {
        try
        {
            // If the appropriate accessor for the current file type is not open, then open it
            if (parser == null)
            {
                if (type == Type.TXT)
                    parser = new TxtParser(file);
                else if (type == Type.DOCX)
                    parser = new DocxParser(file);
            }

            return true;
        }
        catch (FileNotFoundException | Docx4JException exception)
        {
            System.out.printf("DocumentView.accessFile: %s\n", exception.toString());
            return false;
        }
    }
}
