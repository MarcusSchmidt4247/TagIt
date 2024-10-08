/* TagIt
 * TaggerModel.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.models;

import com.github.marcusschmidt4247.tagit.IOManager;
import com.github.marcusschmidt4247.tagit.miscellaneous.ManagedFolder;
import com.github.marcusschmidt4247.tagit.miscellaneous.TagNode;

import java.util.Vector;

public class TaggerModel
{
    private final ManagedFolder folder;
    public ManagedFolder getFolder() { return folder; }
    /**
     * Gets the location of the current <code>ManagedFolder</code> directory.
     * @return an absolute file path
     */
    public String getPath() { return folder.getFullPath(); }

    private final TagNode tagTreeRoot;
    public TagNode getTreeRoot() { return tagTreeRoot; }

    private final Vector<String> files = new Vector<>();
    private int currFileIndex = -1;

    public TaggerModel(ManagedFolder folder)
    {
        this.folder = folder;
        tagTreeRoot = new TagNode(folder);
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
            boolean successful = IOManager.renameFile(folder.getFullPath(), files.get(currFileIndex), name);
            if (successful)
                files.set(currFileIndex, name);
        }
    }

    public void deleteCurrentFile()
    {
        if (!files.isEmpty() && currFileIndex < files.size())
        {
            IOManager.deleteFile(folder.getFullPath(), files.get(currFileIndex));
            files.removeElementAt(currFileIndex);

            if (currFileIndex >= files.size())
                currFileIndex = 0;
        }
    }
}
