/* TagIt
 * TagNode.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

import com.github.marcusschmidt4247.tagit.Database;
import com.github.marcusschmidt4247.tagit.WindowManager;
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
    /**
     * Attempts to move this tag and its children to another part of the tree. This might cause a ripple effect of files being re-tagged,
     * and might be interrupted by the user. If successful, the database will also be updated with the new tree structure.
     * @param newParent the node to which this one should be moved
     */
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
                if (WindowManager.confirmationDialog("New Tag", header, description))
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

    /**
     * Gets a textual representation of the path to this node from the tree root. The tag for each node along the path will be separated
     * by "->" and terminate with this node's tag.
     * @return the description of the path to this node
     */
    public String getTagPath()
    {
        if (parent.get() == null)
            return null;
        else
        {
            String parentPath = parent.get().getTagPath();
            return (parentPath != null) ? String.format("%s->%s", parentPath, tag.getValue()) : tag.getValue();
        }
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
    private void activateChildNode(boolean on)
    {
        parentActivationWeight += on ? 1 : -1;
        activateNode(on);
    }
    public boolean isActive() { return activationWeight > 0; }
    /**
     * Checks whether this node is enabled directly, or indirectly because of an enabled parent.
     * @return <code>true</code> if enabled directly; <code>false</code> otherwise
     */
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

    private ManagedFolder folder = null;
    /**
     * Gets location of <code>ManagedFolder</code> directory that owns this node.
     * @return the absolute path to <code>ManagedFolder</code> directory
     */
    public String getDirectory()
    {
        if (isRoot())
            return folder.getFullPath();
        else
            return getRoot().getDirectory();
    }

    /**
     * Class constructor for the root node. The root node is not a tag - it is the parent for all top-level tags (called root tags).
     * @param folder this tree's owner
     */
    public TagNode(ManagedFolder folder)
    {
        parent.set(null);
        tag = new SimpleStringProperty("root");
        this.folder = folder;
    }

    /**
     * Class constructor for new tags.
     * @param parent the node for which this will become a leaf
     * @param tag the name describing this node
     */
    public TagNode(final TagNode parent, final String tag) { this(parent, tag, -1); }

    /**
     * Class constructor for preexisting tags.
     * @param parent the node for which this will become a leaf
     * @param tag the name describing this node
     * @param id the unique number assigned by the database
     */
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
        if (other != null && id != -1 && other.getId() != -1)
            return id == other.getId();
        else
            return false;
    }

    /**
     * Finds the node in this tag's tree equivalent to <code>item</code>.
     * @param item the counterpart of the node to search for
     * @return the equivalent <code>TagNode</code>; <code>null</code> if not found
     */
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

    /**
     * Searches this node's subtree for a path matching <code>lineage</code>.
     * @param lineage the path of expected tag IDs from one of this node's children to the target
     * @return the node at the end of the path; <code>null</code> if not found
     */
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

    /**
     * Traverses the subtree rooted at this node and creates a depth-first list of IDs.
     * @return a string of comma-separated integers
     */
    public String getSubtreeIds()
    {
        StringBuilder idList = new StringBuilder();
        idList.append(id).append(',');
        getChildren().forEach(child -> idList.append(child.getSubtreeIds()).append(','));
        idList.deleteCharAt(idList.length()-1);
        return idList.toString();
    }
}
