package com.example.tagger;

import java.util.Vector;

public class TaggerModel
{
    private final String PATH;
    public String getPath() { return PATH; }

    private final TagNode TAG_TREE_ROOT;
    public TagNode getTreeRoot() { return TAG_TREE_ROOT; }

    private final Vector<String> FILES = new Vector<>();
    private int currFileIndex = -1;
    public String firstFile()
    {
        if (!FILES.isEmpty())
        {
            currFileIndex = 0;
            return FILES.getFirst();
        }
        else
            return null;
    }
    public String nextFile()
    {
        if (!FILES.isEmpty())
        {
            currFileIndex++;
            if (currFileIndex >= FILES.size())
                currFileIndex = 0;

            return FILES.get(currFileIndex);
        }
        else
            return null;
    }
    public String prevFile()
    {
        if (!FILES.isEmpty())
        {
            currFileIndex--;
            if (currFileIndex < 0)
                currFileIndex = FILES.size() - 1;

            return FILES.get(currFileIndex);
        }
        return null;
    }
    public void setFiles(Vector<String> files)
    {
        currFileIndex = 0;
        FILES.removeAllElements();
        FILES.addAll(files);
    }
    public void printFiles()
    {
        System.out.println("****\nCurrent files:");
        for (String file : FILES)
        {
            System.out.println(file);
        }
        System.out.println("****\n");
    }

    public TaggerModel(final String PATH)
    {
        this.PATH = PATH;
        TAG_TREE_ROOT = new TagNode(PATH);
    }
}
