/* TagIt
 * Parser.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.parsers;

import javafx.scene.text.Font;

import java.util.Collections;
import java.util.Vector;

public abstract class Parser
{
    abstract public int getNextPageNum();
    abstract public boolean setNextPage(int targetPage, final int maxChars);
    abstract public ParserResults readNextPage(final Font font, final int maxChars);
    abstract public void close();

    // Search backwards from the end of the string for a good character to end on
    protected static String neatCutoff(String text)
    {
        if (text == null || text.isEmpty())
            return text;

        final char cutoffChar = ' ';
        final Vector<Character> altCutoffChars = new Vector<>();
        Collections.addAll(altCutoffChars, '-', '_', ',', '.', ';', ':', '/', '\\');

        final int maxCutoff = 100;
        int index, cutoffIndex = -1, altCutoffIndex = -1;
        for (index = text.length() - 1; index > 0 && index >= text.length() - maxCutoff; index--)
        {
            if (text.charAt(index) == cutoffChar)
            {
                cutoffIndex = index;
                break;
            }
            else if (altCutoffIndex == -1 && altCutoffChars.contains(text.charAt(index)))
                altCutoffIndex = index;
        }

        String result = "";
        if (cutoffIndex != -1)
        {
            // Make the space inclusive so that the next page doesn't start with it
            if (cutoffIndex < text.length() - 1)
                cutoffIndex++;
            result = text.substring(0, cutoffIndex);
        }
        else if (altCutoffIndex != -1)
            result = text.substring(0, altCutoffIndex);
        else if (index != 0)
            result = text;

        return result;
    }
}
