package com.example.tagger;

import java.io.*;

public class ReadWriteManager
{
    public static TagNode readTags(final String PATH)
    {
        TagNode root = new TagNode(null, "root");
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(String.format("%s/tags.txt", PATH))))
        {
            String line = bufferedReader.readLine();
            TagNode parent = root;
            int treeDepth = 0;
            while (line != null)
            {
                // Regex of expected line format: -*>.+
                int newDepth = line.indexOf('>');
                String tag = line.substring(newDepth + 1);

                if (newDepth > treeDepth)
                {
                    parent = parent.getChildren().getLast();
                    treeDepth++;
                }
                else
                {
                    while (newDepth < treeDepth)
                    {
                        parent = parent.getParent();
                        treeDepth--;
                    }
                }

                parent.getChildren().add(new TagNode(parent, tag));
                line = bufferedReader.readLine();
            }
        }
        catch (IOException e)
        {
            // If the expected tag file is missing, try creating a new one before throwing an exception if that also doesn't work
            System.out.println("Tag file missing, creating new empty file");
            File tagFile = new File(String.format("%s/tags.txt", PATH));
            try
            {
                //noinspection ResultOfMethodCallIgnored
                tagFile.createNewFile();
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }
        return root;
    }

    /* Return a list of all files in the provided directory that pass the filter (needs a compatible extension),
     * or return null if the path is not a valid directory or does not permit its files to be read */
    public static File[] getFilesInDir(final String PATH)
    {
        File directory = new File(PATH);
        if (directory.isDirectory())
        {
            try
            {
                return directory.listFiles(new FilenameFilter()
                {
                    @Override
                    public boolean accept(File dir, String name)
                    {
                        return name.matches(".+[.](jpeg|jpg|png)$");
                    }
                });
            }
            catch (SecurityException exception)
            {
                System.out.printf("ReadWriteManager.getFilesInDir: Does not have permission to read files in directory \"%s\"", PATH);
                return null;
            }
        }
        else
        {
            System.out.printf("ReadWriteManager.getFilesInDir: Path \"%s\" does not lead to a valid directory", PATH);
            return null;
        }
    }
}
