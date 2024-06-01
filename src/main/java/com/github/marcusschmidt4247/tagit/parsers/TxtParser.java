/* TagIt
 * TxtParser.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.parsers;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TxtParser extends Parser
{
    private final File file;
    private FileReader fileReader;

    private int nextPage;
    public int getNextPage() { return nextPage; }

    public TxtParser(File file) throws FileNotFoundException
    {
        this.file = file;
        fileReader = new FileReader(file);
        nextPage = 0;
    }

    @Override
    public boolean setNextPage(int targetPage, final int maxChars)
    {
        // Unless reading a future page, reset the file reader to the beginning of the file
        if (targetPage >= nextPage || resetFileReader())
        {
            // Move forward one page at a time until the start of the target page has been reached
            while (nextPage < targetPage)
            {
                long skipped = -1;
                try
                {
                    skipped = fileReader.skip(maxChars);
                }
                catch (IOException exception)
                {
                    System.out.printf("TxtParser.setNextPage: %s\n", exception.toString());
                }

                if (skipped == -1)
                    return false;
                else if (skipped == maxChars)
                    nextPage++;
                else
                {
                    // If less than a full page could be skipped, then either this or the previously read page must be the last one
                    int lastPage = nextPage;
                    if (skipped == 0)
                        lastPage--;

                    // In order to go back to the start of the last full page, reset again and call another move to the last page
                    return resetFileReader() && setNextPage(lastPage, maxChars);
                }
            }

            return true;
        }
        else
            return false;
    }

    @Override
    public ParserResults readNextPage(final Font font, final int maxChars)
    {
        try
        {
            // Attempt to read the maximum number of characters for a page
            char[] buffer = new char[maxChars];
            int length = fileReader.read(buffer, 0, maxChars);
            if (length != -1)
            {
                nextPage++;
                // If less than a full page was able to be read, mark that this page must be the last one
                boolean eof = (length < maxChars);

                Text content = new Text(new String(buffer, 0, length));
                content.setFont(font);
                return new ParserResults(content, eof);
            }
            else
            {
                // If there was nothing to be read, the previous page must be the last one
                System.out.println("TxtParser.readPage: Already reached EOF");
                return null;
            }
        }
        catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void close()
    {
        try { fileReader.close(); }
        catch (IOException exception) { System.out.println("TxtParser.close: Failed to close fileReader"); }
    }

    private boolean resetFileReader()
    {
        try
        {
            nextPage = 0;
            close();
            fileReader = new FileReader(file);
            return true;
        }
        catch (FileNotFoundException exception)
        {
            System.out.println("TxtParser.resetFileReader: File not found");
            return false;
        }
    }
}
