/* TagIt
 * ParserResults.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.parsers;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.Vector;

public class ParserResults
{
    private final Vector<TextFlow> nodes;
    public Vector<TextFlow> getNodes() { return nodes; }

    private final boolean endOfFile;
    public boolean isEndOfFile() { return endOfFile; }

    public ParserResults(Vector<TextFlow> nodes, boolean endOfFile)
    {
        this.nodes = nodes;
        this.endOfFile = endOfFile;
    }

    public ParserResults(Text node, boolean endOfFile)
    {
        nodes = new Vector<>();
        TextFlow textFlow = new TextFlow();
        textFlow.getChildren().add(node);
        nodes.add(textFlow);

        this.endOfFile = endOfFile;
    }
}
