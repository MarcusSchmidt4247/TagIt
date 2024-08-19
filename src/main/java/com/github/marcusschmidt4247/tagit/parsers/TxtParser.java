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
import java.util.Vector;

public class TxtParser extends Parser
{
    private final File file;
    private FileReader fileReader;
    private int filePos;

    private int nextPage;
    public int getNextPageNum() { return nextPage; }

    private final Vector<Integer> pages;
    private String carryover;

    public TxtParser(File file) throws FileNotFoundException
    {
        this.file = file;
        fileReader = new FileReader(file);
        filePos = 0;
        nextPage = 0;
        pages = new Vector<>();
        pages.add(filePos);
        carryover = "";
    }

    @Override
    public boolean setNextPage(int targetPage, final int maxChars)
    {
        // Unless reading a future page, reset the file reader to the beginning of the file
        if (targetPage >= nextPage || resetFileReader())
        {
            // Skip as close to the target page as has already been indexed
            int destPage = (targetPage < pages.size()) ? targetPage : (pages.size() - 1);
            int destPos = pages.get(destPage);
            long skipped = -1;
            try
            {
                skipped = fileReader.skip(destPos);
            }
            catch (IOException exception)
            {
                System.out.printf("TxtParser.setNextPage: %s\n", exception.toString());
            }

            if (skipped != -1 && skipped == destPos)
            {
                filePos = destPos;
                nextPage = destPage;

                // If there are any pages left to traverse, read them until the destination or end of file is reached
                while (nextPage < targetPage)
                {
                    ParserResults page = readNextPage(Font.font(1.0), maxChars);
                    if (page == null)
                        return false;
                    else if (page.isEndOfFile())
                    {
                        int lastPage = nextPage - 1;
                        return resetFileReader() && setNextPage(lastPage, maxChars);
                    }
                }

                return true;
            }
            // If less than the full amount could be skipped, then the file has likely been truncated and the pages need to be re-indexed
            else if (skipped != -1)
            {
                pages.clear();
                pages.add(0);
                return resetFileReader() && setNextPage(targetPage, maxChars);
            }
        }

        return false;
    }

    @Override
    public ParserResults readNextPage(final Font font, final int maxChars)
    {
        try
        {
            // Attempt to read the maximum number of characters for a new page, or exactly the length of a known page
            int pageLen = (nextPage + 1 < pages.size()) ? (pages.get(nextPage + 1) - pages.get(nextPage)) : maxChars;
            int bufferLen = pageLen - carryover.length();
            char[] buffer = new char[bufferLen];
            int length = fileReader.read(buffer, 0, bufferLen);
            if (length != -1)
            {
                filePos += length;
                String text = carryover + new String(buffer, 0, length);
                length += carryover.length();
                carryover = "";
                nextPage++;

                // Determine whether the end of the file has been reached before a full page could be read
                boolean eof = (length < pageLen);
                // If the file continues onto another page
                if (!eof)
                {
                    int pageStart = filePos;
                    // Find a good place to stop this page if it doesn't already end with a space
                    if (buffer[bufferLen - 1] != ' ')
                    {
                        String trimmed = neatCutoff(text);
                        carryover = text.substring(trimmed.length());
                        text = trimmed;
                        pageStart -= length - text.length();
                    }

                    // Record the position where the next page starts if it has not been already
                    if (nextPage >= pages.size())
                        pages.add(pageStart);
                }

                Text content = new Text(text);
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
            filePos = 0;
            nextPage = 0;
            carryover = "";
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
