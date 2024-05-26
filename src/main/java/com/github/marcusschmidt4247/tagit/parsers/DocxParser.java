/* TagIt
 * DocxParser.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.parsers;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.docx4j.Docx4J;
import org.docx4j.TraversalUtil;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Body;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;

import java.io.File;
import java.util.List;
import java.util.Vector;

public class DocxParser extends Parser
{
    private final WordprocessingMLPackage mainDocument;

    private int nextPage;
    public int getNextPage() { return nextPage; }

    public DocxParser(File file) throws Docx4JException
    {
        mainDocument = Docx4J.load(file);
        nextPage = 0;
    }

    @Override
    public boolean setNextPage(int targetPage, final int maxChars)
    {
        this.nextPage = targetPage;
        return true;
    }

    // Reference: https://github.com/plutext/docx4j/blob/VERSION_11_4_12/docx4j-samples-docx4j/src/main/java/org/docx4j/samples/DisplayMainDocumentPartXml.java
    public void logParts() { System.out.println(XmlUtils.marshaltoString(mainDocument.getMainDocumentPart().getJaxbElement(), true, true)); }

    // Reference: https://github.com/plutext/docx4j/blob/master/docx4j-samples-docx4j/src/main/java/org/docx4j/samples/OpenMainDocumentAndTraverse.java
    @Override
    public ParserResults readNextPage(final Font font, final int maxChars)
    {
        Vector<Text> nodes = new Vector<>();
        boolean[] eof = new boolean[1]; // array or holder class needed so that a reference can be passed-by-value
        try
        {
            Body body = mainDocument.getMainDocumentPart().getJaxbElement().getBody();
            new TraversalUtil(body, new TraversalUtil.Callback()
            {
                int len = 0;
                private boolean firstParagraph = true;
                private javafx.scene.text.Text nextRun = null;

                @Override
                public List<Object> apply(Object object)
                {
                    object = XmlUtils.unwrap(object);

                    // At the start of a new paragraph, insert a new line into the list of nodes unless it's the first paragraph
                    if (object instanceof org.docx4j.wml.P)
                    {
                        if (firstParagraph)
                            firstParagraph = false;
                        else
                            nodes.add(new javafx.scene.text.Text("\n"));
                    }
                    // At the start of a new run, prepare a next 'nextRun' object
                    else if (object instanceof org.docx4j.wml.R)
                    {
                        nextRun = new javafx.scene.text.Text();

                        RPr properties = ((R) object).getRPr();
                        if (properties != null)
                        {
                            FontPosture italic = FontPosture.REGULAR;
                            if (properties.getI() != null && properties.getI().isVal())
                                italic = FontPosture.ITALIC;

                            FontWeight bold = FontWeight.NORMAL;
                            if (properties.getB() != null && properties.getB().isVal())
                                bold = FontWeight.BOLD;

                            nextRun.setFont(Font.font(font.getName(), bold, italic, font.getSize()));
                        }
                        else
                            nextRun.setFont(font);
                    }
                    // At the start of text, insert the text into 'nextRun' and add it to the list of nodes
                    else if (object instanceof org.docx4j.wml.Text)
                    {
                        String text = ((org.docx4j.wml.Text) object).getValue();
                        if (len + text.length() > maxChars)
                            text = text.substring(0, text.length() - (len + text.length() - maxChars));
                        len += text.length();

                        nextRun.setText(text);
                        nodes.add(nextRun);
                    }

                    return null;
                }

                @Override
                public void walkJAXBElements(Object parent)
                {
                    List<Object> children = getChildren(parent);
                    if (children != null)
                    {
                        for (Object child : children)
                        {
                            if (len >= maxChars)
                                return;

                            this.apply(child);

                            child = XmlUtils.unwrap(child);
                            if (this.shouldTraverse(child))
                                walkJAXBElements(child);
                        }

                        // If the last child of the document body has been read, then the end of file has been reached
                        if (parent instanceof Body)
                            eof[0] = true;
                    }
                }

                @Override public List<Object> getChildren(Object o) { return TraversalUtil.getChildrenImpl(o); }

                @Override public boolean shouldTraverse(Object o) { return true; }
            });
        }
        catch (Exception e) { throw new RuntimeException(e); }

        nextPage++;
        return new ParserResults(nodes, eof[0]);
    }

    @Override public void close() { }
}
