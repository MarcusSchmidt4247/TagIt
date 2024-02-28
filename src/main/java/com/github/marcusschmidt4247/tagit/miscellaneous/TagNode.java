/* TagIt
 * TagNode.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.IOManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.util.Vector;

public class TagNode
{
    private final SimpleObjectProperty<TagNode> parent = new SimpleObjectProperty<>();
    public SimpleObjectProperty<TagNode> parentProperty() { return parent; }
    public TagNode getParent() { return parent.get(); }
    public void changeParent(TagNode newParent)
    {
        if (newParent.addChild(this))
        {
            // Update this tag's parentage in the object and in the database
            TagNode prevParent = parent.get();
            parent.set(newParent);
            Database.updateTagParentage(this);
            // Then remove it from the previous parent (will start a chain of events requiring up-to-date info)
            prevParent.removeChild(this);
        }
    }

    private final StringProperty tag;
    public StringProperty tagProperty() { return tag; }
    public String getTag() { return tag.getValue(); }
    public void setTag(String tag) { this.tag.setValue(tag); }

    private boolean fetchedChildren = false;
    public boolean fetchedChildren() { return fetchedChildren; }

    private final ObservableList<TagNode> children = FXCollections.observableArrayList();
    private void fetchChildren()
    {
        fetchedChildren = true;
        if (isRoot())
            Database.getRootTags(this, children);
        else
            Database.getChildTags(this, children);
    }
    public ObservableList<TagNode> childrenProperty() { return children; }
    public ObservableList<TagNode> getChildren()
    {
        if (!fetchedChildren)
            fetchChildren();
        return FXCollections.unmodifiableObservableList(children);
    }
    public boolean addChild(TagNode child)
    {
        /* If this tag is a leaf node, then all the files tagged with it need to be re-tagged with the new child tag.
         * Confirm that the user is okay with this before adding the child */
        boolean added = true;
        if (!isRoot() && isLeaf())
        {
            Vector<String> files = Database.getTaggedFiles(this);
            if (!files.isEmpty())
            {
                String header = String.format("There are %d files tagged with \"%s\" that will now also be tagged with \"%s\".", files.size(), tag.get(), child.getTag());
                String description = "All tags a file is associated with must be part of a chain that ends with a childless tag.";
                if (IOManager.confirmAction("New Tag", header, description))
                {
                    for (String file : files)
                    {
                        Database.deleteFileTag(file, this);
                        Database.addFileTag(file, child);
                    }
                }
                else
                    added = false;
            }
        }

        if (added)
        {
            // Ensure the list of this tag's children has been fetched
            if (!fetchedChildren)
                fetchChildren();
            // And then add to it
            children.add(child);
        }
        return added;
    }
    public void removeChild(TagNode child) { children.remove(child); }
    public boolean hasChild(String name)
    {
        for (TagNode child : getChildren())
        {
            if (child.getTag().equals(name))
                return true;
        }
        return false;
    }

    public boolean isLeaf()
    {
        if (!fetchedChildren)
            return Database.isLeafTag(this);
        else
            return children.isEmpty();
    }

    public String getTagPath()
    {
        if (parent.get() == null)
            return null;
        else
            return (parent.get().getTagPath() != null) ? String.format("%s->%s", parent.get().getTagPath(), tag.getValue()) : tag.getValue();
    }

    private int id = -1;
    public int getId() { return id; }
    public void setId(int id)
    {
        if (this.id == -1)
            this.id = id;
    }

    private int activationWeight;
    private int parentActivationWeight;
    public void activateNode(boolean on)
    {
        activationWeight += on ? 1 : -1;
        for (TagNode child : getChildren())
            child.activateChildNode(on);
    }
    public void activateChildNode(boolean on)
    {
        parentActivationWeight += on ? 1 : -1;
        activateNode(on);
    }
    public boolean isActive() { return activationWeight > 0; }
    public boolean isSelfActivated() { return activationWeight > parentActivationWeight; }

    private int exclusionWeight = 0;
    public void excludeNode(boolean on)
    {
        exclusionWeight += on ? 1 : -1;
        for (TagNode child : getChildren())
            child.excludeNode(on);
    }
    public boolean isExcluded() { return exclusionWeight > 0; }

    public boolean isRoot() { return (parent.get() == null); }
    public TagNode getRoot()
    {
        if (isRoot())
            return this;
        else
            return parent.get().getRoot();
    }

    // Root node constructor
    public TagNode()
    {
        parent.set(null);
        tag = new SimpleStringProperty("root");
    }

    // General constructor (should not be used for the root node)
    public TagNode(final TagNode parent, final String TAG) { this(parent, TAG, -1); }

    // General constructor (should not be used for the root node)
    public TagNode(final TagNode parent, String tag, int id)
    {
        this.parent.set(parent);
        this.tag = new SimpleStringProperty(tag);
        this.id = id;
        activationWeight = 0;
        parentActivationWeight = 0;
    }

    public boolean equals(TagNode other)
    {
        if (other == null)
            return false;
        else if (id != -1 || other.getId() != -1)
            return id == other.getId();
        else
            return tag.getValue().equals(other.getTag());
    }

    /* Starting from the root TagNode of a (sub)tree that is equivalent to the one containing the provided TreeItem,
     * return the TagNode that corresponds to the TreeItem, or null if it does not exist. */
    public TagNode findNode(TreeItem<String> item)
    {
        if (item == null)
            return null;
        else if (item.getParent() == null)
            return (item.getValue().equals(tag.getValue())) ? this : null;
        else
        {
            // Construct a path to this node by traversing backwards up the TreeView
            Vector<String> path = new Vector<>();
            while (item != null && item.getParent() != null) // exclude the root element
            {
                path.insertElementAt(item.getValue(),0);
                item = item.getParent();
            }

            // Attempt to follow this path forward through the TagNode tree
            TagNode node = this;
            for (String step : path)
            {
                boolean foundMatch = false;
                for (TagNode child : node.getChildren())
                {
                    if (child.getTag().equals(step))
                    {
                        node = child;
                        foundMatch = true;
                        break;
                    }
                }

                // If there was no match found for this step, the trees don't match and the node cannot be found
                if (!foundMatch)
                    return null;
            }

            return node;
        }
    }

    /* Search the (sub)tree starting with this node for a chain of nodes with IDs equal to those in the provided vector
     * and return the TagNode at the end if it exists, or null if not. */
    public TagNode findNode(Vector<Integer> lineage)
    {
        TagNode currentNode = this;
        for (int id : lineage)
        {
            boolean foundMatch = false;
            for (TagNode child : currentNode.getChildren())
            {
                if (child.getId() == id)
                {
                    foundMatch = true;
                    currentNode = child;
                    break;
                }
            }

            if (!foundMatch)
                return null;
        }

        return currentNode;
    }

    // Return a string of comma-separated IDs for all the nodes in the subtree that is rooted at this node
    public String getSubtreeIds()
    {
        StringBuilder idList = new StringBuilder();
        idList.append(id).append(',');
        getChildren().forEach(child -> idList.append(child.getSubtreeIds()).append(','));
        idList.deleteCharAt(idList.length()-1);
        return idList.toString();
    }

    public boolean delete()
    {
        // Delete this tag's children first, and halt the process if deleting any of them is unsuccessful
        if (!children.isEmpty())
        {
            for (int i = children.size() - 1; i >= 0; i--)
            {
                if (!children.get(i).delete())
                    return false;
            }
        }

        // Before deleting this tag, check whether there are any files that will be left untagged without it
        boolean proceed = false;
        Vector<String> orphanedFiles = Database.getUniqueFiles(this);
        if (orphanedFiles.isEmpty())
            proceed = true;
        else
        {
            // If there are files that will be orphaned, the user must choose to cancel, delete the files, or re-tag them
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.setTitle("Orphaned Files");
            warning.setHeaderText(String.format("\"%s\" is the only tag for %d files.", tag.get(), orphanedFiles.size()));
            warning.setContentText("These files will be inaccessible without at least one tag. They must either be deleted or moved to another tag.");
            // Get rid of the default button and add three custom  buttons with the appropriate choices
            warning.getButtonTypes().removeFirst();
            warning.getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.RIGHT));
            ButtonType deleteButton = new ButtonType("Delete Files", ButtonBar.ButtonData.RIGHT);
            warning.getButtonTypes().add(deleteButton);
            ButtonType newTagButton = new ButtonType("Select Tag", ButtonBar.ButtonData.RIGHT);
            warning.getButtonTypes().add(newTagButton);
            // Set the button to select a new tag for these files as the default button
            ((Button) warning.getDialogPane().lookupButton(newTagButton)).setDefaultButton(true);
            // Show the dialog and record the user's choice
            warning.showAndWait();

            if (warning.getResult() == deleteButton)
            {
                orphanedFiles.forEach(IOManager::deleteFile);
                proceed = true;
            }
            else if (warning.getResult() == newTagButton)
            {
                // If the user chose to re-tag the files, open a tag selector window and re-tag the files in the database with the user's selection
                TagNode selection = IOManager.selectTag(getRoot(), this);
                if (selection != null)
                {
                    for (String orphan : orphanedFiles)
                    {
                        Database.deleteFileTag(orphan, this);
                        Database.addFileTag(orphan, selection);
                    }
                    proceed = true;
                }
            }
        }

        // If there was no problem with orphaned files or if the user resolved the problem, delete this tag
        if (proceed)
        {
            Database.deleteTag(this);
            if (parent.get() != null)
                parent.get().removeChild(this);
        }
        return proceed;
    }
}
