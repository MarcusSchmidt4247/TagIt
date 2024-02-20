package com.example.tagger;

import com.example.tagger.miscellaneous.SearchCriteria;
import com.example.tagger.miscellaneous.TagNode;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Vector;

public class Database
{
    // Ensure the database file exists
    public static void verify(final String PATH)
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
                    try (Connection connection = connect(database.getAbsolutePath()))
                    {
                        if (connection.isValid(5))
                        {
                            // If successful, create the database tables
                            String fileSchema = "CREATE TABLE File(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE, created INTEGER NOT NULL)";
                            String tagSchema = "CREATE TABLE Tag(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE)";
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
        try (Connection connection = connect(tag.getRootPath()))
        {
            String sql = String.format("INSERT INTO Tag(name) VALUES(\"%s\")", tag.getTag());
            Statement statement = connection.createStatement();
            statement.execute("PRAGMA foreign_keys = ON");
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
        try (Connection connection = connect(root.getRootPath()))
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
        try (Connection connection = connect(parent.getRootPath()))
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
        try (Connection connection = connect(tag.getRootPath()))
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

    public static void renameTag(TagNode tag)
    {
        // A tag with the ID -1 has not been written to the database yet, so it cannot be updated
        if (tag.getId() != -1)
        {
            try (Connection connection = connect(tag.getRootPath()))
            {
                String sql = String.format("UPDATE Tag SET name=\"%s\" WHERE id=%d", tag.getTag(), tag.getId());
                Statement statement = connection.createStatement();
                statement.execute(sql);
                statement.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            System.out.println("Database.renameTag: Tag ID = -1");
    }

    public static void updateTagParentage(TagNode tag)
    {
        // If the TagNode does not have a parent then it is the tree's root and NOT a tag in and of itself, so there's nothing to update
        if (tag.getParent() != null && tag.getId() != -1)
        {
            try (Connection connection = connect(tag.getRootPath()))
            {
                Statement statement = connection.createStatement();
                statement.execute("PRAGMA foreign_keys = ON");

                // If the tag's new parentage makes it a root tag (its parent is the root of the tag tree), then remove it as a child from the TagParentage table
                String sql;
                if (tag.getParent().getParent() == null)
                    sql = String.format("DELETE FROM TagParentage WHERE child_id=%d", tag.getId());
                else
                {
                    // Otherwise, check whether the tag has already been inserted as a child into the TagParentage table
                    ResultSet resultSet = statement.executeQuery(String.format("SELECT count(child_id) FROM TagParentage WHERE child_id=%d", tag.getId()));
                    // If it has, just update the row
                    if (resultSet.next() && resultSet.getInt(1) > 0)
                        sql = String.format("UPDATE TagParentage SET parent_id=%d WHERE child_id=%d", tag.getParent().getId(), tag.getId());
                    // Otherwise, insert it into the table
                    else
                        sql = String.format("INSERT INTO TagParentage VALUES (%d, %d)", tag.getParent().getId(), tag.getId());
                }
                statement.execute(sql);
                statement.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            System.out.println("Database.updateTagParent: Tag's parent is null or ID = -1");
    }

    public static void deleteTag(TagNode tag)
    {
        if (tag.getId() != -1)
        {
            try (Connection connection = connect(tag.getRootPath()))
            {
                Statement statement = connection.createStatement();
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute(String.format("DELETE FROM Tag WHERE id=%d", tag.getId()));
                statement.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            System.out.println("Database.deleteTag: Tag ID = -1");
    }

    // Return every tag associated with the provided file
    public static Vector<TagNode> getFileTags(TagNode root, String fileName)
    {
        Vector<TagNode> tags = new Vector<>();

        try (Connection connection = connect(root.getRootPath()))
        {
            // Get this file's ID
            Statement statement = connection.createStatement();
            String sql = String.format("SELECT id FROM File WHERE name=\"%s\"", fileName);
            ResultSet results = statement.executeQuery(sql);
            if (results.next())
            {
                int fileId = results.getInt(1);

                // Get the IDs for every tag associated with this file
                sql = String.format("SELECT id FROM Tag JOIN FileTags ON id=tag_id WHERE file_id=%d", fileId);
                results = statement.executeQuery(sql);
                Vector<Integer> tagIds = new Vector<>();
                while (results.next())
                    tagIds.add(results.getInt(1));

                // For each tag ID, reconstruct its lineage and follow it to the equivalent TagNode
                for (int id : tagIds)
                {
                    Vector<Integer> lineage = new Vector<>();
                    lineage.add(id);
                    int currentId = id;
                    do
                    {
                        sql = String.format("SELECT parent_id FROM TagParentage WHERE child_id=%d", currentId);
                        results = statement.executeQuery(sql);
                        if (results.next())
                        {
                            currentId = results.getInt(1);
                            lineage.insertElementAt(currentId, 0);
                        }
                        else
                            currentId = -1;
                    } while (currentId != -1);

                    TagNode tag = root.findNode(lineage);
                    if (tag != null)
                        tags.add(tag);
                    else
                        System.out.println("Database.getFileTags: Unable to follow lineage to TagNode");
                }
            }
            else
                System.out.printf("Database.getFileTags: Unable to retrieve file ID for \"%s\"\n", fileName);
            statement.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

        return tags;
    }

    // Return the list of names of every file in the managed directory that meets the tag criteria
    public static Vector<String> getTaggedFiles(SearchCriteria searchCriteria)
    {
        Vector<String> files = new Vector<>();
        if (!searchCriteria.getIncludeAny().isEmpty() || !searchCriteria.getIncludeAll().isEmpty())
        {
            try (Connection connection = connect(searchCriteria.getPath()))
            {
                String table = "File";
                String sql;
                Statement statement = connection.createStatement();

                if (!searchCriteria.getExcludeIds().isEmpty())
                {
                    // Create a comma-separated list of the IDs for every tag that disqualifies a file from being included in the search results
                    StringBuilder excludeList = new StringBuilder();
                    for (Integer id : searchCriteria.getExcludeIds())
                        excludeList.append(id).append(",");
                    excludeList.deleteCharAt(excludeList.length() - 1); // gets rid of the trailing comma

                    // Create a temporary table that duplicates File but excludes rows associated with an excluded tag ID
                    table = "CandidateFiles";
                    sql = String.format("CREATE TEMPORARY TABLE %s AS SELECT File.* FROM File LEFT JOIN ", table) +
                          String.format("(SELECT * FROM FileTags WHERE tag_id IN (%s)) ON id=file_id WHERE tag_id IS NULL", excludeList);
                    statement.execute(sql);
                }

                if (searchCriteria.isAnyMatch())
                {
                    // Create a comma-separated list of the tag IDs for which there only needs to be one match to include a file in the search results
                    StringBuilder includeList = new StringBuilder();
                    for (Integer id : searchCriteria.getIncludeAny())
                        includeList.append(id).append(",");
                    includeList.deleteCharAt(includeList.length() - 1); // gets rid of the trailing comma

                    // Select the name of every file that is associated with one of the tags
                    sql = String.format("SELECT DISTINCT name FROM %s JOIN FileTags ON id=file_id WHERE tag_id IN (%s)", table, includeList);
                }
                else
                {
                    /* If a file must be associated with EVERY selected tag, then for each tag create a temporary table of the list of file IDs associated with it or with ANY of its children.
                     * This can also be thought of as searching for any match within the subtree of every selected TagNode. */
                    StringBuilder joins = new StringBuilder();
                    joins.append("Criteria0 ON id=id0");
                    for (int i = 0; i < searchCriteria.getIncludeAll().size(); i++)
                    {
                        sql = String.format("CREATE TEMPORARY TABLE Criteria%d AS SELECT DISTINCT file_id as id%d FROM FileTags WHERE tag_id IN (%s)", i, i, searchCriteria.getIncludeAll().get(i).getSubtreeIds());
                        statement.execute(sql);

                        if (i > 0)
                            joins.append(String.format(" JOIN Criteria%d ON id%d=id%d", i, i-1, i));
                    }

                    // Join the tables to select the files that are associated with all the selected tags
                    sql = String.format("SELECT DISTINCT name FROM %s JOIN %s", table, joins);
                }

                switch (searchCriteria.getSortMethod())
                {
                    case NAME:
                        sql = sql.concat(" ORDER BY name ASC");
                        break;
                    case AGE:
                        sql = sql.concat(" ORDER BY created ASC");
                        break;
                    case IMPORT:
                        sql = sql.concat(" ORDER BY id ASC");
                        break;
                    case RANDOM:
                        sql = sql.concat(" ORDER BY RANDOM()");
                        break;
                    default:
                        System.out.println("Database.getTaggedFiles: Unrecognized sort method");
                }

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
    public static void saveFile(final String path, String fileName, long fileCreatedMillis, Vector<TagNode> tags)
    {
        if (!tags.isEmpty())
        {
            try (Connection connection = connect(path))
            {
                Statement statement = connection.createStatement();
                statement.execute("PRAGMA foreign_keys = ON");

                // Convert the file's creation time from milliseconds to seconds since the epoch, or retrieve the current time if the file's attribute was unavailable
                long created;
                if (fileCreatedMillis != -1)
                    created = fileCreatedMillis / 1000;
                else
                {
                    ResultSet result = statement.executeQuery("SELECT unixepoch('now')");
                    created = result.getLong(0);
                }

                // Insert the file name and time created into the File table
                String sql = String.format("INSERT INTO File(name, created) VALUES(\"%s\", %d)", fileName, created);
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
                    System.out.println("Database.saveFile: Unable to retrieve file ID, tags not inserted");

                statement.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            System.out.println("Database.saveFile: Cannot save file without any tags");
    }

    // Associate the provided file and tag by inserting a row into the FileTags table
    public static void addFileTag(String file, TagNode tag)
    {
        if (tag.getId() != -1)
        {
            try (Connection connection = connect(tag.getRootPath()))
            {
                Statement statement = connection.createStatement();
                statement.execute("PRAGMA foreign_keys = ON");

                String sql = String.format("SELECT id FROM File WHERE name=\"%s\"", file);
                ResultSet results = statement.executeQuery(sql);
                if (results.next())
                {
                    int fileId = results.getInt(1);
                    sql = String.format("INSERT INTO FileTags VALUES(%d, %d)", fileId, tag.getId());
                    statement.execute(sql);
                }
                else
                    System.out.printf("Database.addFileTag: Unable to retrieve file ID for \"%s\"\n", file);

                statement.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            System.out.println("Database.addFileTag: Tag ID = -1");
    }

    // Dissociate the provided file and tag by deleting a row from the FileTags table
    public static void deleteFileTag(String file, TagNode tag)
    {
        if (tag.getId() != -1)
        {
            try (Connection connection = connect(tag.getRootPath()))
            {
                Statement statement = connection.createStatement();
                statement.execute("PRAGMA foreign_keys = ON");

                String sql = String.format("SELECT id FROM File WHERE name=\"%s\"", file);
                ResultSet results = statement.executeQuery(sql);
                if (results.next())
                {
                    int fileId = results.getInt(1);
                    sql = String.format("DELETE FROM FileTags WHERE file_id=%d AND tag_id=%d", fileId, tag.getId());
                    statement.execute(sql);
                }
                else
                    System.out.printf("Database.deleteFileTag: Unable to retrieve file ID for \"%s\"\n", file);

                statement.close();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            System.out.println("Database.deleteFileTag: Tag ID = -1");
    }

    // Return the list of names of every file in the managed directory that is only tagged with the provided tag
    public static Vector<String> getUniqueFiles(TagNode tag)
    {
        Vector<String> files = new Vector<>();

        if (tag.getId() != -1)
        {
            try (Connection connection = connect(tag.getRootPath()))
            {
                String sql = "SELECT name FROM File JOIN FileTags ON id=FileTags.file_id " +
                        String.format("JOIN (SELECT file_id FROM FileTags WHERE tag_id=%d) as ids ON id=ids.file_id ", tag.getId()) +
                        "GROUP BY id HAVING count(tag_id)=1";

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
        else
            System.out.println("Database.getUniqueFiles: Tag ID = -1");

        return files;
    }

    public static boolean renameFileInDatabase(String path, String oldName, String newName)
    {
        try (Connection connection = connect(path))
        {
            String sql = String.format("UPDATE File SET name=\"%s\" WHERE name=\"%s\"", newName, oldName);
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
            return true;
        }
        catch (SQLException e)
        {
            // If the name cannot be updated in the database, attempt to revert the actual file's name to keep everything consistent
            System.out.println("Database.renameFile: Unable to rename file in database");
            return false;
        }
    }

    public static void deleteFileFromDatabase(String path, String fileName)
    {
        try (Connection connection = connect(path))
        {
            Statement statement = connection.createStatement();
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute(String.format("DELETE FROM File WHERE name=\"%s\"", fileName));
            statement.close();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Connection connect(String path) throws SQLException
    {
        String url = String.format("jdbc:sqlite:%s/%s", path, "database.db");
        return DriverManager.getConnection(url);
    }
}
