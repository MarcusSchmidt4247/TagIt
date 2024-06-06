/* TagIt
 * DocxParser.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.parsers;

import javafx.geometry.Insets;
import javafx.scene.text.*;
import javafx.scene.text.Text;
import org.docx4j.Docx4J;
import org.docx4j.TraversalUtil;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class DocxParser extends Parser
{
    private final static double FONT_SIZE_RATIO = 1.5; // XML font size relative to JavaFX Font size (e.g. 150 in XML is equivalent to 100 in JavaFX)
    private final static double EMPTY_SPACE_RATIO = 25.0; // XML empty space (e.g. indents, line spacing) relative to JavaFX empty space
    private final static double MIN_NEWLINE_HEIGHT = 300.0; // The amount of line spacing in XML that warrants a newline in JavaFX (480 is the exact height)

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

    // Reference: https://github.com/plutext/docx4j/blob/VERSION_11_4_12/docx4j-samples-docx4j/src/main/java/org/docx4j/samples/DisplayMainDocumentPartXml.java
    public void logParts() { System.out.println(XmlUtils.marshaltoString(mainDocument.getMainDocumentPart().getJaxbElement(), true, true)); }

    @Override
    public boolean setNextPage(int targetPage, final int maxChars)
    {
        boolean successful = true;
        // If the target page is already indexed, the 'nextPage' variable can just be set
        nextPage = targetPage;
        // Otherwise, index new pages until the target page (or end of file) has been reached
        if (targetPage >= pages.size())
        {
            successful = traverseDocument(maxChars, new DocxTraversalCallback(maxChars, pages.getLast())
            {
                @Override void newPage(PageStart pageStart)
                {
                    pages.add(pageStart);
                    pageLen = 0;
                }

                @Override void endOfFile() { nextPage = pages.size() - 1; }

                @Override boolean finished() { return (targetPage < pages.size()); }
            });
        }
        return successful;
    }

    // Reference: https://github.com/plutext/docx4j/blob/master/docx4j-samples-docx4j/src/main/java/org/docx4j/samples/OpenMainDocumentAndTraverse.java
    @Override
    public ParserResults readNextPage(final Font font, final int maxChars)
    {
        Vector<TextFlow> flows = new Vector<>();
        boolean[] eof = new boolean[1]; // array or holder class is needed so that a reference can be passed-by-value

        boolean successful = traverseDocument(maxChars, new DocxTraversalCallback(maxChars, pages.get(nextPage))
        {
            private TextFlow currentFlow;
            private Text currentRun;
            private double carryoverSpacing;

            @Override
            public void init(PageStart startPage)
            {
                super.init(startPage);
                currentFlow = new TextFlow();
                currentRun = null;
                carryoverSpacing = 0.0;
            }

            @Override
            void newPage(PageStart pageStart)
            {
                // Only record the start of a new page if it has not been already (recall that after page 0 is read, the start of both page 0 and 1 have been recorded)
                if (nextPage + 1 >= pages.size())
                        pages.add(pageStart);
            }

            @Override
            void newParagraph(P paragraph, boolean continuing)
            {
                StringBuilder newLine = new StringBuilder();

                PPr properties = paragraph.getPPr();
                // If no paragraph properties are listed, set them to the defaults
                if (properties == null)
                    setFlowProperties(TextAlignment.LEFT, 0.0, 0.0, 0.0);
                else
                {
                    // Check for paragraph alignment
                    TextAlignment alignment = TextAlignment.LEFT;
                    if (properties.getJc() != null)
                        alignment = convertAlignment(properties.getJc().getVal());

                    // Check for paragraph indentation
                    double leftIndent = 0.0, rightIndent = 0.0;
                    if (properties.getInd() != null)
                    {
                        if (properties.getInd().getLeft() != null)
                            leftIndent = properties.getInd().getLeft().doubleValue();
                        if (properties.getInd().getRight() != null)
                            rightIndent = properties.getInd().getRight().doubleValue();
                    }

                    // Check for paragraph spacing
                    if (properties.getSpacing() == null)
                        setFlowProperties(alignment, leftIndent, rightIndent, 0.0);
                    else
                    {
                        double lineSpacing = 0.0;
                        if (properties.getSpacing().getLine() != null)
                            lineSpacing = properties.getSpacing().getLine().doubleValue() / EMPTY_SPACE_RATIO;
                        setFlowProperties(alignment, leftIndent, rightIndent, lineSpacing);

                        // Add the spacing from after the previous paragraph to any spacing that is before this paragraph
                        double paragraphSpacing = carryoverSpacing;
                        if (properties.getSpacing().getBefore() != null)
                            paragraphSpacing += properties.getSpacing().getBefore().doubleValue();

                        // Insert the number of newlines that is roughly equivalent to this amount of spacing
                        int lines = (int) (paragraphSpacing / MIN_NEWLINE_HEIGHT);
                        if (lines > 0)
                        {
                            char[] spacingChars = new char[lines];
                            Arrays.fill(spacingChars, '\n');
                            newLine.append(spacingChars);
                        }

                        carryoverSpacing = 0.0;
                    }

                    // For first-line indentation, prioritize bulleted lists
                    if (properties.getPStyle() != null && properties.getPStyle().getVal().equals("ListParagraph"))
                    {
                        if (properties.getNumPr() != null && properties.getNumPr().getIlvl() != null)
                        {
                            // Determine the bullet point character from the list level
                            int listLevel = properties.getNumPr().getIlvl().getVal().intValue();
                            char bulletPoint = '-';
                            if (listLevel % 3 == 1)
                                bulletPoint = '*';
                            else if (listLevel % 3 == 2)
                                bulletPoint = 'º';

                            // Indent the line according to the list level and add the bullet point with a small buffer
                            char[] indent = new char[listLevel];
                            Arrays.fill(indent, '\t');
                            newLine.append(indent).append(bulletPoint).append("  ");
                        }
                        else
                            System.out.println("DocxTraversalCallback.newParagraph: Unable to retrieve ListParagraph level");
                    }
                    // Otherwise, only indent the first line of this paragraph if it didn't start on the previous page
                    else if (!continuing && properties.getInd() != null && properties.getInd().getFirstLine() != null)
                    {
                        int len = (int) (properties.getInd().getFirstLine().doubleValue() / EMPTY_SPACE_RATIO);
                        if (len > 0)
                        {
                            char[] indent = new char[len];
                            Arrays.fill(indent, ' ');
                            newLine.append(indent);
                        }
                    }
                }

                /* A new paragraph needs to be put on its own line. A new TextFlow will do this automatically, but if this is not
                 * the beginning of a TextFlow, then manually insert a newline character before any possible indentation. */
                if (!currentFlow.getChildren().isEmpty())
                    newLine.insert(0, '\n');

                if (!newLine.isEmpty())
                    currentFlow.getChildren().add(new Text(newLine.toString()));
            }

            @Override
            void endParagraph(P paragraph)
            {
                // If the first paragraph of a subsequent TextFlow was empty, insert a newline retroactively
                if (currentFlow.getChildren().isEmpty())
                    currentFlow.getChildren().add(new Text("\n"));

                // Record the amount of spacing that should be after this paragraph
                PPr properties = paragraph.getPPr();
                if (properties != null && properties.getSpacing() != null && properties.getSpacing().getAfter() != null)
                    carryoverSpacing = properties.getSpacing().getAfter().doubleValue();
            }

            @Override
            void newRun(R run)
            {
                currentRun = new Text();

                RPr properties = run.getRPr();
                if (properties != null)
                {
                    FontPosture italic = FontPosture.REGULAR;
                    if (properties.getI() != null && properties.getI().isVal())
                        italic = FontPosture.ITALIC;

                    FontWeight bold = FontWeight.NORMAL;
                    if (properties.getB() != null && properties.getB().isVal())
                        bold = FontWeight.BOLD;

                    if (properties.getU() != null)
                        currentRun.setUnderline(true);

                    if (properties.getStrike() != null && properties.getStrike().isVal())
                        currentRun.setStrikethrough(true);

                    String fontName = font.getName();
                    if (properties.getRFonts() != null)
                    {
                        if (properties.getRFonts().getAscii() != null)
                            fontName = properties.getRFonts().getAscii();
                        else if (properties.getRFonts().getHAnsi() != null)
                            fontName = properties.getRFonts().getHAnsi();
                        else if (properties.getRFonts().getCs() != null)
                            fontName = properties.getRFonts().getCs();
                    }

                    double fontSize = font.getSize();
                    if (properties.getSz() != null)
                        fontSize = properties.getSz().getVal().doubleValue() / FONT_SIZE_RATIO;

                    currentRun.setFont(Font.font(fontName, bold, italic, fontSize));
                }
                else
                    currentRun.setFont(font);
            }

            @Override
            void newText(String text)
            {
                currentRun.setText(text);
                currentFlow.getChildren().add(currentRun);
            }

            @Override
            void endOfFile()
            {
                // Only record the EoF if the start page has been found and something is being read
                if (foundStart())
                {
                    eof[0] = true;
                    if (currentFlow != null)
                        flows.add(currentFlow);
                }
            }

            @Override
            boolean finished()
            {
                boolean finished = (pageLen >= maxChars);
                if (finished && currentFlow != null)
                {
                    flows.add(currentFlow);
                    currentFlow = null;
                }
                return finished;
            }

            private void setFlowProperties(TextAlignment alignment, double leftIndent, double rightIndent, double lineSpacing)
            {
                // Check if any of these properties don't match the current TextFlow
                double leftPadding = leftIndent / EMPTY_SPACE_RATIO;
                double rightPadding = rightIndent / EMPTY_SPACE_RATIO;
                if (!alignment.equals(currentFlow.getTextAlignment()) || Math.abs(leftPadding - currentFlow.getPadding().getLeft()) > 0.1 ||
                    Math.abs(rightPadding - currentFlow.getPadding().getRight()) > 0.1 || Math.abs(lineSpacing - currentFlow.getLineSpacing()) > 0.1)
                {
                    // Start a new TextFlow unless the current one is empty (such as when the first paragraph has properties that need to be set)
                    if (!currentFlow.getChildren().isEmpty())
                    {
                        flows.add(currentFlow);
                        currentFlow = new TextFlow();
                    }
                    currentFlow.setTextAlignment(alignment);
                    currentFlow.setLineSpacing(lineSpacing);
                    if (rightPadding > 0.01 || leftPadding > 0.01)
                        currentFlow.setPadding(new Insets(0, rightPadding, 0, leftPadding));
                }
            }

            // Return the TextAlignment enum value (used by JavaFX) that corresponds to the provided JcEnumeration enum value (from the XML)
            private TextAlignment convertAlignment(JcEnumeration alignment)
            {
                return switch (alignment)
                {
                    case JcEnumeration.BOTH -> TextAlignment.JUSTIFY;
                    case JcEnumeration.CENTER -> TextAlignment.CENTER;
                    case JcEnumeration.RIGHT -> TextAlignment.RIGHT;
                    default -> TextAlignment.LEFT;
                };
            }
        });

        if (successful)
        {
            nextPage++;
            return new ParserResults(flows, eof[0]);
        }
        else
            return null;
    }

    @Override public void close() { }

    // Use the provided callback method to traverse the .docx file, and if the StartPage cannot be found then re-index the pages and try again
    private boolean traverseDocument(final int maxChars, DocxTraversalCallback callback)
    {
        Body body = mainDocument.getMainDocumentPart().getJaxbElement().getBody();
        int attempts = 0;
        do
        {
            new TraversalUtil(body, callback);

            if (!callback.foundStart())
            {
                System.out.println("DocxParser.traverseDocument: Failed to locate start of page");
                // Reset 'pages'
                pages.clear();
                pages.add(new PageStart(null, 0));
                // Re-index the pages up to the current position
                setNextPage(nextPage, maxChars);
                // Reset the callback with the new start page
                callback.init(pages.get(nextPage));
                attempts++;
            }
        } while (!callback.foundStart() && attempts < 2);

        return callback.foundStart();
    }

    private abstract static class DocxTraversalCallback implements TraversalUtil.Callback
    {
        protected int pageLen;
        private final int maxPageLen;
        private PageStart startPage;

        private boolean traversing;
        public boolean foundStart() { return !traversing; }

        private String paraId;
        private int paraOffset;

        public DocxTraversalCallback(final int maxPageLen, final PageStart startPage)
        {
            super();
            this.maxPageLen = maxPageLen;
            init(startPage);
        }

        public void init(PageStart startPage)
        {
            this.startPage = startPage;
            paraId = startPage.paraId;
            traversing = (startPage.paraId != null);
            pageLen = 0;
            paraOffset = 0;
        }

        // Handler functions that must be implemented by subclasses
        abstract void newPage(PageStart pageStart);
        abstract void endOfFile();
        abstract boolean finished();

        // Optional handler functions
        void newParagraph(P paragraph, boolean continuing) { }
        void endParagraph(P paragraph) { }
        void newRun(R run) { }
        void newText(String text) { }

        @Override
        public List<Object> apply(Object object)
        {
            object = XmlUtils.unwrap(object);

            // Skip objects until we find the first paragraph of the start page
            if (traversing)
            {
                if (object instanceof org.docx4j.wml.P para && startPage.paraId.equals(((org.docx4j.wml.P) object).getParaId()))
                {
                    traversing = false;
                    newParagraph(para, true);
                }
            }
            else
            {
                if (object instanceof P para)
                {
                    paraId = para.getParaId();
                    paraOffset = 0;

                    newParagraph(para, false);
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

                // If every child has been traversed without satisfying the end condition
                if (!finished())
                {
                    // Check if this is the end of a paragraph that wasn't skipped
                    if (!traversing && parent instanceof P)
                        endParagraph((P) parent);
                    // Check if this is the end of the document
                    else if (parent instanceof Body)
                        endOfFile();
                }
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
                /* Alert the handler for the start of a new page (note that if this text is also the end of a paragraph,
                 * the next page will be indexed as beginning at the very end of this paragraph) */
                newPage(new PageStart(paraId, paraOffset));
                // Unless the conditions have been met to stop processing the file, process the text on the next page
                if (!finished())
                    processText(parts[1]);
            }
        }
    }
}
