package com.example.tagger.gui;

import com.example.tagger.Database;
import com.example.tagger.miscellaneous.TagNode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.CheckBoxTreeCell;
import org.controlsfx.control.CheckTreeView;

import java.util.Vector;

public class DynamicCheckTreeView extends CheckTreeView<String>
{
    public interface ActionCallback
    {
        void action(TagNode node, boolean checked, boolean alt);
    }

    public enum Mode { DEFAULT, LEAF_CHECK, SINGLE_CHECK }

    private static final String TEMP_TREE_ITEM_CHILD = "temp_cb_tree_item_child";

    private TagNode rootNode = null;
    private Mode mode;
    private final Vector<Integer> removedCheckedTagIDs = new Vector<>();

    public void init(TagNode root) { init(root, Mode.DEFAULT); }

    public void initSingleCheck(TagNode root, TagNode preselectedNode)
    {
        // Initialize the tree like normal
        init(root, Mode.SINGLE_CHECK);

        // If the equivalent TreeItem to 'preselectedNode' can be found, remove it from the tree and set its parent to be checked
        if (preselectedNode != null)
        {
            TreeItem<String> currentItem = findItem(preselectedNode, true);
            if (currentItem != null)
            {
                CheckBoxTreeItem<String> parentItem = ((CheckBoxTreeItem<String>) currentItem.getParent());
                parentItem.setSelected(true);
                parentItem.getChildren().remove(currentItem);
            }
        }
    }

    public void init(TagNode root, Mode mode)
    {
        if (rootNode == null)
        {
            this.mode = mode;

            /* Set up a custom cell factory that supports mixing TreeItem and CheckBoxTreeItem nodes.
             * Source for cell factory code: https://blog.idrsolutions.com/mixed-treeview-nodes-javafx/ */
            setCellFactory(factory -> new CheckBoxTreeCell<>()
            {
                @Override
                public void updateItem(String item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if (empty || item == null)
                    {
                        setText(null);
                        setGraphic(null);
                    }
                    else if (!(getTreeItem() instanceof CheckBoxTreeItem))
                    {
                        setGraphic(null);
                    }
                }
            });

            rootNode = root;
            populateTreeItem(getRoot(), rootNode);
        }
    }

    public TreeItem<String> findItem(TagNode tag, boolean expandPath)
    {
        // Attempt to find the TreeItem that is equivalent to 'preselectedNode'
        TreeItem<String> currentItem = getRoot();
        String[] path = tag.getTagPath().split("->");
        for (String step : path)
        {
            boolean foundMatch = false;
            for (TreeItem<String> childItem : currentItem.getChildren())
            {
                if (childItem.getValue().equals(step))
                {
                    foundMatch = true;
                    currentItem = childItem;

                    if (expandPath)
                        childItem.setExpanded(true);

                    break;
                }
            }

            if (!foundMatch)
                return null;
        }

        return currentItem;
    }

    /* Populate a newly expanded TreeItem with children matching those in the provided TagNode
     * (both arguments should reference equivalent nodes in their respective trees) */
    private void populateTreeItem(TreeItem<String> treeItem, TagNode node)
    {
        // This function is expected to only be called the first time treeItem is expanded, so get rid of its placeholder child
        if (!treeItem.getChildren().isEmpty() && treeItem.getChildren().getFirst().getValue().equals(TEMP_TREE_ITEM_CHILD))
            treeItem.getChildren().removeFirst();

        // Then add CheckBoxTreeItems for its actual children
        node.getChildren().forEach(child -> treeItem.getChildren().add(newTreeItem(child)));

        // Create a listener on the TagNode's list of children so that the tree item will be updated if it changes
        node.childrenProperty().addListener((ListChangeListener<TagNode>) change ->
        {
            while (change.next())
            {
                for (TagNode addedNode : change.getAddedSubList())
                    treeItem.getChildren().add(newTreeItem(addedNode));

                for (TagNode removedNode : change.getRemoved())
                {
                    for (TreeItem<String> childItem : treeItem.getChildren())
                    {
                        if (childItem.getValue().equals(removedNode.getTag()))
                        {
                            // If the tree item that is going to be removed is a checked CheckBoxTreeItem, uncheck it first
                            if (childItem instanceof CheckBoxTreeItem<String> && ((CheckBoxTreeItem<String>) childItem).isSelected())
                            {
                                // Also record the tag's ID so that it can optionally be fetched later
                                removedCheckedTagIDs.add(removedNode.getId());
                                ((CheckBoxTreeItem<String>) childItem).setSelected(false);
                            }

                            treeItem.getChildren().remove(childItem);
                            break;
                        }
                    }
                }
            }

            // If this tree only allows leaf nodes to be checked and the last child was just removed, change the TreeItem to CheckBoxTreeItem
            if (mode == Mode.LEAF_CHECK && node.getChildren().isEmpty())
                updateTreeItem(treeItem, node);
        });
    }

    private TreeItem<String> newTreeItem(TagNode node)
    {
        TreeItem<String> treeItem;
        if (mode == Mode.LEAF_CHECK && !node.isLeaf())
            treeItem = new TreeItem<>();
        else
        {
            treeItem = new CheckBoxTreeItem<>();
            ((CheckBoxTreeItem<String>) treeItem).setIndependent(true);
        }

        treeItem.valueProperty().bind(node.tagProperty());

        if (!node.isLeaf())
            configUnexpandedTreeItem(treeItem, node);
        else
        {
            /* If this tag is currently a leaf and doesn't need to be expandable, add a listener to its list of children
             * so that it will be configured to be expandable if children are added to it */
            node.childrenProperty().addListener(new ListChangeListener<>()
            {
                @Override
                public void onChanged(Change<? extends TagNode> change)
                {
                    if (!node.getChildren().isEmpty() && treeItem.getChildren().isEmpty())
                    {
                        node.childrenProperty().removeListener(this);

                        // If only leaves can be checked and this tree item is no longer a leaf, replace its CheckBoxTreeItem with TreeItem
                        if (mode == Mode.LEAF_CHECK && treeItem instanceof CheckBoxTreeItem<String> && treeItem.getParent() != null)
                            updateTreeItem(treeItem, node);
                        // Otherwise, just configure the CheckBoxTreeItem so that it can be expanded
                        else
                            configUnexpandedTreeItem(treeItem, node);
                    }
                }
            });
        }

        return treeItem;
    }

    private void updateTreeItem(TreeItem<String> treeItem, TagNode node)
    {
        TreeItem<String> parent = treeItem.getParent();
        if (parent != null)
        {
            int index = parent.getChildren().indexOf(treeItem);
            parent.getChildren().remove(treeItem);
            treeItem = newTreeItem(node);
            parent.getChildren().add(index, treeItem);

            if (!treeItem.isLeaf())
                treeItem.setExpanded(true);
        }
        else
            System.out.printf("DynamicCheckTreeView.updateTreeItem: \"%s\" has no parent\n", treeItem.getValue());
    }

    private void configUnexpandedTreeItem(TreeItem<String> treeItem, TagNode node)
    {
        /* If this new child is not a leaf node, it needs a listener on its expanded property that will call populateTagTree for it,
         * as well as being given a placeholder tree item that will give the user the option to expand it */
        treeItem.expandedProperty().addListener(new ChangeListener<>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1)
            {
                if (observableValue.getValue())
                {
                    populateTreeItem(treeItem, node);
                    // Stop listening for any further changes now that the children have been added to the tree
                    treeItem.expandedProperty().removeListener(this);
                }
            }
        });
        CheckBoxTreeItem<String> invisibleItem = new CheckBoxTreeItem<>(TEMP_TREE_ITEM_CHILD);
        treeItem.getChildren().add(invisibleItem);
    }

    // Returns true if all newly checked and unchecked tree items could be processed, and false if any of them were checked when their associated TagNode was deleted
    public boolean processCheckedDelta(Vector<TreeItem<String>> added, Vector<TreeItem<String>> removed, TagNode root, ActionCallback callback)
    {
        boolean success = true;

        // Moving tags sometimes results in a duplicate removed TreeItem event being thrown, so filter those out before processing
        for (TreeItem<String> addedItem : added)
            removed.removeIf(removedItem -> (removedItem.getValue().equals(addedItem.getValue())));

        // For every newly checked tree item, perform the positive callback action on its associated TagNode
        for (TreeItem<String> addedItem : added)
        {
            TagNode node = root.findNode(addedItem);
            if (node != null)
                callback.action(node, true, false);
            else
                System.out.printf("DynamicCheckTreeView.processCheckedDelta: Unable to find TagNode for checked tree item \"%s\"\n", addedItem.getValue());
        }

        // For every newly unchecked tree item, perform the negative callback action on its associated TagNode
        for (TreeItem<String> treeItem : removed)
        {
            TagNode node = root.findNode(treeItem);
            if (node != null)
                callback.action(node, false, false);
            else
            {
                /* If the TagNode can't be found, then it was moved or deleted while checked. The tag's ID should have been stored so that its new location
                 * (if there is one) can be retrieved from the database. */
                if (!removedCheckedTagIDs.isEmpty())
                {
                    Vector<Integer> lineage = Database.getTagLineage(removedCheckedTagIDs.removeFirst());
                    node = root.findNode(lineage);
                    if (node != null)
                    {
                        /* If the TagNode was found in a new location, then it has been moved and not deleted. Its new tree item will be in an
                         * unchecked state and needs to be checked, but the TagNode needs its negative callback action to be performed first.
                         * In case this behavior is tree-dependent, alert the callback that this is an alternate callback action. */
                        callback.action(node, false, true);

                        // Attempt to check the TagNode's new tree item in its new location (which should trigger its own positive callback on the associated TagNode)
                        TreeItem<String> newItem = findItem(node, true);
                        if (newItem instanceof CheckBoxTreeItem<String>)
                            ((CheckBoxTreeItem<String>) newItem).setSelected(true);
                        else
                            System.out.printf("DynamicCheckTreeView.processCheckedDelta: Unable to find the new tree item for \"%s\"\n", treeItem.getValue());

                        // Perform a positive alternate callback action in case there's a tree-dependent behavior for after the tree item has (hopefully) been checked
                        callback.action(node, true, true);
                    }
                    else
                        // If the TagNode wasn't found in a new location, then it has been deleted. Indicate with the return value that a negative callback action wasn't performed for it
                        success = false;
                }
                else
                    System.out.printf("DynamicCheckTreeView.processCheckedDelta: Unable to fetch tag ID for disconnected TreeItem \"%s\"\n", treeItem.getValue());
            }
        }
        return success;
    }
}
