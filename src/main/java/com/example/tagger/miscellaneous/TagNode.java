package com.example.tagger.miscellaneous;

import com.example.tagger.Database;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import java.util.Vector;

public class TagNode
{
    private final SimpleObjectProperty<TagNode> parent = new SimpleObjectProperty<>();
    public SimpleObjectProperty<TagNode> parentProperty() { return parent; }
    public TagNode getParent() { return parent.get(); }
    public void changeParent(TagNode newParent)
    {
        parent.get().getChildren().remove(this);
        parent.set(newParent);
        parent.get().getChildren().add(this);

        Database.updateTagParentage(this);
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
            if (parent.get() == null)
                Database.getRootTags(this, children);
            else
                Database.getChildTags(this, children);
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
            return Database.isLeafTag(this);
        else
            return children.isEmpty();
    }

    private final String ROOT_PATH;
    public String getRootPath() { return ROOT_PATH; }

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

    // Root node constructor
    public TagNode(final String ROOT_PATH)
    {
        this.ROOT_PATH = ROOT_PATH;
        parent.set(null);
        tag = new SimpleStringProperty("root");
    }

    public TagNode(final TagNode parent, final String TAG) { this(parent, TAG, -1); }

    // General constructor (should not be used for the root node unless you want it to be displayed in 'tagPath')
    public TagNode(final TagNode parent, String tag, int id)
    {
        this.parent.set(parent);
        ROOT_PATH = parent.getRootPath();
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

    public void delete()
    {
        if (!children.isEmpty())
        {
            for (int i = children.size() - 1; i >= 0; i--)
                children.get(i).delete();
        }

        Database.deleteTag(this);
        if (parent.get() != null)
            parent.get().getChildren().remove(this);
    }
}