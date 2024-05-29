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

    private record PageStart(String paraId, int offset) { }
    private final Vector<PageStart> pages = new Vector<>();

    public DocxParser(File file) throws Docx4JException
    {
        mainDocument = Docx4J.load(file);
        nextPage = 0;
        pages.add(new PageStart(null, 0));
    }

    @Override
    public boolean setNextPage(int targetPage, final int maxChars)
    {
        // If the target page is already indexed, the 'nextPage' variable can just be set
        nextPage = targetPage;
        if (targetPage >= pages.size())
        {
            // Otherwise, index new pages until the target page (or end of file) has been reached
            Body body = mainDocument.getMainDocumentPart().getJaxbElement().getBody();
            new TraversalUtil(body, new DocxTraversalCallback(maxChars, pages.getLast())
            {
                @Override void newPage(PageStart pageStart)
                {
                    pages.add(pageStart);
                    pageLen = 0;
                }

                @Override void newParagraph() { }

                @Override void newRun(R run) { }

                @Override void newText(String text) { }

                @Override void endOfFile() { nextPage = pages.size() - 1; }

                @Override boolean finished() { return (targetPage < pages.size()); }
            });
        }
        return true;
    }

    // Reference: https://github.com/plutext/docx4j/blob/VERSION_11_4_12/docx4j-samples-docx4j/src/main/java/org/docx4j/samples/DisplayMainDocumentPartXml.java
    public void logParts() { System.out.println(XmlUtils.marshaltoString(mainDocument.getMainDocumentPart().getJaxbElement(), true, true)); }

    // Reference: https://github.com/plutext/docx4j/blob/master/docx4j-samples-docx4j/src/main/java/org/docx4j/samples/OpenMainDocumentAndTraverse.java
    @Override
    public ParserResults readNextPage(final Font font, final int maxChars)
    {
        Vector<Text> nodes = new Vector<>();
        boolean[] eof = new boolean[1]; // array or holder class is needed so that a reference can be passed-by-value
        try
        {
            Body body = mainDocument.getMainDocumentPart().getJaxbElement().getBody();
            new TraversalUtil(body, new DocxTraversalCallback(maxChars, pages.get(nextPage))
            {
                private javafx.scene.text.Text nextRun = null;

                @Override void newPage(PageStart pageStart) { pages.add(pageStart); }

                @Override
                void newParagraph()
                {
                    if (!nodes.isEmpty())
                        nodes.add(new javafx.scene.text.Text("\n"));
                }

                @Override
                void newRun(R run)
                {
                    nextRun = new javafx.scene.text.Text();

                    RPr properties = run.getRPr();
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

                @Override
                void newText(String text)
                {
                    nextRun.setText(text);
                    nodes.add(nextRun);
                }

                @Override void endOfFile() { eof[0] = true; }

                @Override boolean finished() { return pageLen >= maxChars; }
            });
        }
        catch (Exception e) { throw new RuntimeException(e); }

        nextPage++;
        return new ParserResults(nodes, eof[0]);
    }

    @Override public void close() { }

    private abstract static class DocxTraversalCallback implements TraversalUtil.Callback
    {
        protected int pageLen = 0;
        private final int maxPageLen;

        private final PageStart startPage;
        private boolean traversing;

        private String paraId;
        private int paraOffset = 0;

        public DocxTraversalCallback(final int maxPageLen, final PageStart startPage)
        {
            super();
            this.maxPageLen = maxPageLen;
            this.startPage = startPage;
            paraId = startPage.paraId;
            traversing = (startPage.paraId != null);
        }

        // Handler functions that need to be implemented by subclasses
        abstract void newPage(PageStart pageStart);
        abstract void newParagraph();
        abstract void newRun(R run);
        abstract void newText(String text);
        abstract void endOfFile();
        abstract boolean finished();

        @Override
        public List<Object> apply(Object object)
        {
            object = XmlUtils.unwrap(object);

            // Skip objects until we find the first paragraph of the start page
            if (traversing)
            {
                if (object instanceof org.docx4j.wml.P && startPage.paraId.equals(((org.docx4j.wml.P) object).getParaId()))
                    traversing = false;
            }
            else
            {
                if (object instanceof org.docx4j.wml.P)
                {
                    paraId = ((org.docx4j.wml.P) object).getParaId();
                    paraOffset = 0;

                    newParagraph();
                }
                else if (object instanceof org.docx4j.wml.R)
                    newRun((R) object);
                else if (object instanceof org.docx4j.wml.Text)
                {
                    String text = ((org.docx4j.wml.Text) object).getValue();

                    // If this text is in the first paragraph of the start page
                    if (paraId.equals(startPage.paraId))
                    {
                        // Don't register any of this text if it is entirely contained in a part of the paragraph on the previous page
                        if (startPage.offset > paraOffset + text.length())
                        {
                            paraOffset += text.length();
                            return null;
                        }
                        // If any of the text does reach the start page, cut off the beginning
                        else if (startPage.offset > paraOffset)
                        {
                            text = text.substring(startPage.offset - paraOffset);
                            paraOffset = startPage.offset;
                        }
                    }

                    processText(text);
                }
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
                    if (finished())
                        return;

                    this.apply(child);

                    child = XmlUtils.unwrap(child);
                    if (this.shouldTraverse(child))
                        walkJAXBElements(child);
                }

                // If every child of the document body has been traversed without satisfying the finished condition, the end of file must have been reached
                if (parent instanceof Body && !finished())
                    endOfFile();
            }
        }

        @Override public List<Object> getChildren(Object o) { return TraversalUtil.getChildrenImpl(o); }

        @Override public boolean shouldTraverse(Object o) { return true; }

        private void processText(String text)
        {
            int len = text.length();

            // If this text will overflow to the next page, split it at the end of the page
            String[] parts = null;
            if (pageLen + text.length() >= maxPageLen)
            {
                len = maxPageLen - pageLen;
                parts = new String[2];
                parts[0] = text.substring(0, len);
                parts[1] = text.substring(len);
            }

            // Alert the handler for new text
            newText((parts == null) ? text : parts[0]);

            // Update the length of the current page and the location in the current paragraph to reflect the added text
            pageLen += len;
            paraOffset += len;

            // If there's more of this text that overflows to the next page
            if (parts != null)
            {
                // Alert the handler for the start of a new page
                newPage(new PageStart(paraId, paraOffset));
                // Unless the conditions have been met to stop processing the file, process the text on the next page
                if (!finished())
                    processText(parts[1]);
            }
        }
    }
}
