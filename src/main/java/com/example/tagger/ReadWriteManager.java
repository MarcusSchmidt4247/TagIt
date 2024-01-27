package com.example.tagger;

import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Vector;

public class ReadWriteManager
{
    // Ensure the database file exists
    public static void verifyDatabase(final String PATH)
    {
        // Create a file object representing what should be a valid database in the device's file system
        File database = new File(String.format("%s/database.db", PATH));
        if (!database.isFile())
        {
            // If this file object does not represent a valid file, try to create it
            try
            {
                if (database.createNewFile())
                {
                    // If successful, try to connect to it as a SQLite database
                    String url = String.format("jdbc:sqlite:%s", database.getAbsolutePath());
                    try (Connection connection = DriverManager.getConnection(url))
                    {
                        if (connection.isValid(5))
                        {
                            // If successful, create the database tables
                            String fileSchema = "CREATE TABLE File(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)";
                            String tagSchema = "CREATE TABLE Tag(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)";
                            String fileTagsSchema = "CREATE TABLE FileTags(file_id INTEGER NOT NULL," +
                                    "tag_id INTEGER NOT NULL," +
                                    "FOREIGN KEY (file_id) REFERENCES File(id) ON DELETE CASCADE," +
                                    "FOREIGN KEY (tag_id) REFERENCES Tag(id) ON DELETE CASCADE," +
                                    "PRIMARY KEY (file_id, tag_id))";
                            String tagParentageSchema = "CREATE TABLE TagParentage(parent_id INTEGER NOT NULL," +
                                    "child_id INTEGER NOT NULL," +
                                    "FOREIGN KEY (parent_id) REFERENCES Tag(id) ON DELETE CASCADE," +
                                    "FOREIGN KEY (child_id) REFERENCES Tag(id) ON DELETE CASCADE," +
                                    "PRIMARY KEY (parent_id, child_id))";
                            Statement statement = connection.createStatement();
                            statement.execute(fileSchema);
                            statement.execute(tagSchema);
                            statement.execute(fileTagsSchema);
                            statement.execute(tagParentageSchema);
                            statement.close();
                        }
                    }
                }
            }
            catch (IOException | SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    // Save a newly created TagNode to the database
    public static void addTag(TagNode tag)
    {
        String url = String.format("jdbc:sqlite:%s/%s", tag.getRootPath(), "database.db");
        try (Connection connection = DriverManager.getConnection(url))
        {
            String sql = String.format("INSERT INTO Tag(name) VALUES(\"%s\")", tag.getTag());
            Statement statement = connection.createStatement();
            statement.execute(sql, Statement.RETURN_GENERATED_KEYS);

            // Get this tag's ID assigned by the database and pass it along to the TagNode object
            ResultSet keys = statement.getGeneratedKeys();
            int id = (keys.next()) ? keys.getInt(1) : -1;
            tag.setId(id);

            /* A root tag's TagNode is still the child of the root TagNode for the sake of maintaining a single tree with all tags,
               so this TagNode's parent's parent must be null if it is a root tag and doesn't need a TagParentage entry */
            if (tag.getParent() != null && tag.getParent().getParent() != null)
            {
                // If this tag has a parent, insert a row into the TagParentage table to track this relationship
                sql = String.format("INSERT INTO TagParentage VALUES(%d, %d)", tag.getParent().getId(), id);
                statement.execute(sql);
            }
            statement.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Return the list of tags that do not have parent tags
    public static void getRootTags(TagNode root, ObservableList<TagNode> tags)
    {
        tags.clear();
        String url = String.format("jdbc:sqlite:%s/%s", root.getRootPath(), "database.db");
        try (Connection connection = DriverManager.getConnection(url))
        {
            String sql = "SELECT name, id FROM Tag WHERE id NOT IN (SELECT child_id FROM TagParentage) ORDER BY name ASC";
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(sql);
            while (results.next())
                tags.add(new TagNode(root, results.getString(1), results.getInt(2)));
            statement.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Return the list of tags that are children of the provided tag
    public static void getChildTags(TagNode parent, ObservableList<TagNode> children)
    {
        children.clear();
        String url = String.format("jdbc:sqlite:%s/%s", parent.getRootPath(), "database.db");
        try (Connection connection = DriverManager.getConnection(url))
        {
            String sql = "SELECT name, id FROM Tag JOIN TagParentage ON id=child_id WHERE parent_id=? ORDER BY name ASC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, parent.getId());
            if (statement.execute())
            {
                ResultSet results = statement.getResultSet();
                while (results.next())
                    children.add(new TagNode(parent, results.getString(1), results.getInt(2)));
            }
            statement.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Return whether the provided tag has any children tags
    public static boolean isLeafTag(TagNode tag)
    {
        String url = String.format("jdbc:sqlite:%s/%s", tag.getRootPath(), "database.db");
        try (Connection connection = DriverManager.getConnection(url))
        {
            String sql = String.format("SELECT count(parent_id) FROM TagParentage WHERE parent_id=%d", tag.getId());
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(sql);
            boolean leaf = (results.next() && results.getInt(1) == 0);
            statement.close();
            return leaf;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    // Return the list of names of every file in the managed directory that has at least one of its tags active
    public static Vector<String> getTaggedFiles(TagNode root)
    {
        Vector<String> files = new Vector<>();
        Vector<Integer> activeTagIds = root.getActiveNodeIds();
        if (!activeTagIds.isEmpty())
        {
            // Create a comma-separated list of the IDs for every active tag that will be used in the SQL query
            StringBuilder idList = new StringBuilder();
            for (Integer id : activeTagIds)
                idList.append(id).append(",");
            idList.deleteCharAt(idList.length() - 1); // gets rid of the trailing comma

            String url = String.format("jdbc:sqlite:%s/%s", root.getRootPath(), "database.db");
            try (Connection connection = DriverManager.getConnection(url))
            {
                // Select every file's name that is associated with a tag ID that is present in the comma-separated list of searched-for tags
                String sql = String.format("SELECT DISTINCT name FROM File JOIN FileTags ON id=file_id WHERE tag_id IN (%s)", idList);
                Statement statement = connection.createStatement();
                ResultSet results = statement.executeQuery(sql);
                while (results.next())
                    files.add(results.getString(1));
                statement.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        return files;
    }

    // Save a newly imported file to the database
    public static void saveFile(final String PATH, String fileName, Vector<TagNode> tags)
    {
        if (!tags.isEmpty())
        {
            String url = String.format("jdbc:sqlite:%s/%s", PATH, "database.db");
            try (Connection connection = DriverManager.getConnection(url))
            {
                // Insert the filename into the File table
                String sql = String.format("INSERT INTO File(name) VALUES(\"%s\")", fileName);
                Statement statement = connection.createStatement();
                statement.execute(sql, Statement.RETURN_GENERATED_KEYS);

                // Get the ID assigned to this file by the database and insert a row with it and each of its tag's IDs into the FileTags table
                ResultSet keys = statement.getGeneratedKeys();
                if (keys.next())
                {
                    int fileId = keys.getInt(1);
                    for (TagNode tag : tags)
                    {
                        sql = String.format("INSERT INTO FileTags VALUES(%d, %d)", fileId, tag.getId());
                        statement.execute(sql);
                    }
                }
                else
                    System.out.println("ReadWriteManager.saveFile: Unable to retrieve file ID, tags not inserted");

                statement.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
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
                return directory.listFiles(new ExtensionFilter());
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
