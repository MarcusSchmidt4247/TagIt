package com.example.tagger;

import java.util.Vector;

public class TaggerModel
{
    private final String path;
    public String getPath() { return path; }

    private final TagNode tagTreeRoot;
    public TagNode getTreeRoot() { return tagTreeRoot; }

    private final Vector<String> files = new Vector<>();
    private int currFileIndex = -1;
    public String firstFile()
    {
        if (!files.isEmpty())
        {
            currFileIndex = 0;
            return files.getFirst();
        }
        else
            return null;
    }
    public String currentFile()
    {
        if (!files.isEmpty())
            return files.get(currFileIndex);
        else
            return null;
    }
    public String nextFile()
    {
        if (!files.isEmpty())
        {
            currFileIndex++;
            if (currFileIndex >= files.size())
                currFileIndex = 0;

            return files.get(currFileIndex);
        }
        else
            return null;
    }
    public String prevFile()
    {
        if (!files.isEmpty())
        {
            currFileIndex--;
            if (currFileIndex < 0)
                currFileIndex = files.size() - 1;

            return files.get(currFileIndex);
        }
        return null;
    }
    public void setFiles(Vector<String> files)
    {
        currFileIndex = 0;
        this.files.removeAllElements();
        this.files.addAll(files);
    }
    public void printFiles()
    {
        System.out.println("****\nCurrent files:");
        for (String file : files)
        {
            System.out.println(file);
        }
        System.out.println("****\n");
    }

    public TaggerModel(final String path)
    {
        this.path = path;
        tagTreeRoot = new TagNode(path);
    }
}
