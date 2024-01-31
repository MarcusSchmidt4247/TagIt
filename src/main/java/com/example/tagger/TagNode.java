package com.example.tagger;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import java.util.Vector;

public class TagNode
{
    private TagNode parent;
    public TagNode getParent() { return parent; }
    public void changeParent(TagNode newParent)
    {
        parent.getChildren().remove(this);
        parent = newParent;
        parent.getChildren().add(this);
    }

    private final StringProperty tag;
    public StringProperty tagProperty() { return tag; }
    public String getTag() { return tag.getValue(); }
    public void setTag(String tag) { this.tag.setValue(tag); }

    private boolean fetchedChildren = false;
    private final ObservableList<TagNode> children = FXCollections.observableArrayList();
    public ObservableList<TagNode> getChildren() { return getChildren(false); }
    public ObservableList<TagNode> getChildren(boolean noFetch)
    {
        if (!fetchedChildren && !noFetch)
        {
            fetchedChildren = true;
            if (parent == null)
                ReadWriteManager.getRootTags(this, children);
            else
                ReadWriteManager.getChildTags(this, children);
        }
        return children;
    }
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
            return ReadWriteManager.isLeafTag(this);
        else
            return children.isEmpty();
    }

    private final String ROOT_PATH;
    public String getRootPath() { return ROOT_PATH; }

    public String getTagPath()
    {
        if (parent == null)
            return null;
        else
            return (parent.getTagPath() != null) ? String.format("%s->%s", parent.getTagPath(), tag.getValue()) : tag.getValue();
    }

    private int id = -1;
    public int getId() { return id; }
    public void setId(int id)
    {
        if (this.id == -1)
            this.id = id;
    }

    private int activationWeight;

    // Root node constructor
    public TagNode(final String ROOT_PATH)
    {
        this.ROOT_PATH = ROOT_PATH;
        parent = null;
        tag = new SimpleStringProperty("root");
    }

    public TagNode(final TagNode parent, final String TAG) { this(parent, TAG, -1); }

    // General constructor (should not be used for the root node unless you want it to be displayed in 'tagPath')
    public TagNode(final TagNode parent, String tag, int id)
    {
        this.parent = parent;
        ROOT_PATH = parent.getRootPath();
        this.tag = new SimpleStringProperty(tag);
        this.id = id;
        activationWeight = 0;
    }

    public boolean equals(TagNode other)
    {
        if (id != -1 || other.getId() != -1)
            return id == other.getId();
        else
            return tag.getValue().equals(other.getTag());
    }

    public void activateNode()
    {
        activationWeight++;
        for (TagNode child : getChildren())
            child.activateNode();
    }

    public void deactivateNode()
    {
        activationWeight--;
        for (TagNode child : getChildren())
            child.deactivateNode();
    }

    public boolean isActive() { return activationWeight > 0; }

    // Return a list of the activated nodes' IDs that are within this node's subtree
    public Vector<Integer> getActiveNodeIds()
    {
        Vector<Integer> activeNodeIds = new Vector<>();
        if (isActive())
            activeNodeIds.add(id);
        if (fetchedChildren)
        {
            for (TagNode child : children)
                activeNodeIds.addAll(child.getActiveNodeIds());
        }
        return activeNodeIds;
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

    public void delete()
    {
        ReadWriteManager.deleteTag(this);
        if (parent != null)
            parent.getChildren().remove(this);
        if (!children.isEmpty())
        {
            for (int i = children.size() - 1; i >= 0; i--)
                children.get(i).delete();
        }
    }
}
