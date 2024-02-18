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

    public TaggerModel(final String path)
    {
        this.path = path;
        tagTreeRoot = new TagNode(path);
    }

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

    public void renameCurrentFile(String name)
    {
        if (!files.isEmpty() && currFileIndex < files.size())
        {
            boolean successful = ReadWriteManager.renameFile(path, files.get(currFileIndex), name);
            if (successful)
                files.set(currFileIndex, name);
        }
    }

    public void deleteCurrentFile()
    {
        if (!files.isEmpty() && currFileIndex < files.size())
        {
            ReadWriteManager.deleteFile(path, files.get(currFileIndex));
            files.removeElementAt(currFileIndex);

            if (currFileIndex >= files.size())
                currFileIndex = 0;
        }
    }
}
