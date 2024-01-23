package com.example.tagger;

import javafx.scene.control.TreeItem;
import java.util.Vector;

public class TagNode
{
    private final TagNode PARENT;
    public TagNode getParent() { return PARENT; }

    private final String NODE;
    public String getTag() { return NODE; }

    private Vector<TagNode> children = null;
    public Vector<TagNode> getChildren()
    {
        if (children == null)
        {
            if (PARENT == null)
                children = ReadWriteManager.getRootTags(this);
            else
                children = ReadWriteManager.getChildTags(this);
        }
        return children;
    }

    private boolean isLeaf;
    private boolean checkedIsLeaf = false;
    public boolean isLeaf()
    {
        if (!checkedIsLeaf)
        {
            checkedIsLeaf = true;
            isLeaf = ReadWriteManager.isLeafTag(this);
        }
        return isLeaf;
    }

    private final String ROOT_PATH;
    public String getRootPath() { return ROOT_PATH; }

    private String tagPath;
    public String getTagPath() { return tagPath; }

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
        PARENT = null;
        NODE = "root";
        tagPath = null;
        isLeaf = false;
    }

    public TagNode(final TagNode PARENT, final String TAG)
    {
        this(PARENT, TAG, -1);
    }

    // General constructor (should not be used for the root node unless you want it to be displayed in 'tagPath')
    public TagNode(final TagNode PARENT, final String TAG, int id)
    {
        this.PARENT = PARENT;
        ROOT_PATH = PARENT.getRootPath();
        NODE = TAG;
        this.id = id;
        tagPath = (PARENT.getTagPath() != null) ? String.format("%s->%s", PARENT.getTagPath(), NODE) : NODE;
        activationWeight = 0;
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
        for (TagNode child : getChildren())
            activeNodeIds.addAll(child.getActiveNodeIds());
        return activeNodeIds;
    }

    /* Starting from the root TagNode of a (sub)tree that is equivalent to the one containing the provided TreeItem,
     * return the TagNode that corresponds to the TreeItem, or null if it does not exist. */
    public TagNode findNode(TreeItem<String> item)
    {
        if (item == null)
            return null;
        else if (item.getParent() == null)
            return (item.getValue().equals(NODE)) ? this : null;
        else
        {
            // Construct a path to this node by traversing backwards up the TreeView
            Vector<String> path = new Vector<>();
            while (item != null && item.getParent() != null) // exclude the root element
            {
                path.insertElementAt(item.getValue(),0);
                item = item.getParent();
            }

            return findNode(path.toArray(new String[path.size()]));
        }
    }

    public TagNode findNode(String tagChain)
    {
        return findNode(tagChain.split("->"));
    }

    private TagNode findNode(String[] tagChain)
    {
        // Attempt to follow this path forward through the TagNode tree
        TagNode node = this;
        for (String step : tagChain)
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
