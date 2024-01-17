package com.example.tagger;

import java.io.*;
import java.util.Vector;

public class ReadWriteManager
{
    public static TagNode readTags(final String PATH)
    {
        TagNode root = new TagNode();
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
                return directory.listFiles((dir, name) -> name.matches(".+[.](jpe?g|png)$"));
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

    // Return a list of all filenames in the program's storage directory that meet the set of tag criteria
    public static Vector<String> getTaggedFiles(final String PATH, TagNode tagTree)
    {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(String.format("%s/files.txt", PATH))))
        {
            Vector<String> selectedFiles = new Vector<>();
            String line = bufferedReader.readLine();
            while (line != null)
            {
                // Regex of expected line format: .+:(.+,)*.+
                int delimiterIndex = line.indexOf(':');
                String[] fileTagChains = line.substring(delimiterIndex + 1).split(",");
                for (String tagChain : fileTagChains)
                {
                    TagNode node = tagTree.findNode(tagChain);
                    if (node != null && node.isActive())
                    {
                        selectedFiles.add(line.substring(0, delimiterIndex));
                        break;
                    }
                }
                line = bufferedReader.readLine();
            }
            return selectedFiles;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void writeNewFileTags(String fileName, Vector<TagNode> tags, String destination) throws IOException
    {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(destination, true)))
        {
            StringBuilder stringBuilder = new StringBuilder(fileName).append(":");
            for (TagNode tag : tags)
                stringBuilder.append(tag.getTagChain()).append(",");
            stringBuilder.deleteCharAt(stringBuilder.length()-1);
            stringBuilder.append("\n");
            bufferedWriter.write(stringBuilder.toString());
        }
    }
}
