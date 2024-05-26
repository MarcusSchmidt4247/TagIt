/* TagIt
 * ParserResults.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.parsers;

import javafx.scene.text.Text;
import java.util.Vector;

public class ParserResults
{
    private final Vector<Text> nodes;
    public Vector<Text> getNodes() { return nodes; }

    private final boolean endOfFile;
    public boolean isEndOfFile() { return endOfFile; }

    public ParserResults(Vector<Text> nodes, boolean endOfFile)
    {
        this.nodes = nodes;
        this.endOfFile = endOfFile;
    }
}
