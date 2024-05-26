/* TagIt
 * Parser.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.parsers;

import javafx.scene.text.Font;

public abstract class Parser
{
    abstract public int getNextPage();
    abstract public boolean setNextPage(int targetPage, final int maxChars);
    abstract public ParserResults readNextPage(final Font font, final int maxChars);
    abstract public void close();
}
