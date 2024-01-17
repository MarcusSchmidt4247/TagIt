package com.example.tagger;

import javafx.scene.control.TreeItem;

import java.util.Vector;

public class TagNode
{
    private final TagNode PARENT;
    public TagNode getParent() { return PARENT; }

    private final String NODE;
    private final String TAG_CHAIN;
    public String getTag() { return NODE; }
    public String getTagChain() { return TAG_CHAIN; }

    private final Vector<TagNode> CHILDREN = new Vector<>();
    public Vector<TagNode> getChildren() { return CHILDREN; }

    private int activationWeight;

    // Root node constructor
    public TagNode()
    {
        PARENT = null;
        NODE = "root";
        TAG_CHAIN = null;
    }

    // General constructor (should not be used for root node unless you want to see "root" at the beginning of every tag chain)
    public TagNode(final TagNode PARENT, final String TAG)
    {
        this.PARENT = PARENT;
        NODE = TAG;
        TAG_CHAIN = (PARENT != null && PARENT.getTagChain() != null) ? String.format("%s->%s", PARENT.getTagChain(), NODE) : NODE;
        activationWeight = 0;
    }

    public void activateNode()
    {
        activationWeight++;
        for (TagNode child : CHILDREN)
            child.activateNode();
    }

    public void deactivateNode()
    {
        activationWeight--;
        for (TagNode child : CHILDREN)
            child.deactivateNode();
    }

    public boolean isActive() { return activationWeight > 0; }

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
