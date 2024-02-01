package com.example.tagger;

import java.util.Vector;

public class SearchCriteria
{
    private final Vector<Integer> includeIds = new Vector<>();
    public Vector<Integer> getIncludeIds() { return includeIds; }

    private final Vector<Integer> excludeIds = new Vector<>();
    public Vector<Integer> getExcludeIds() { return excludeIds; }

    private final boolean anyMatch;
    public boolean isAnyMatch() { return anyMatch; }

    private final String path;
    public String getPath() { return path; }

    public SearchCriteria(TagNode root, boolean anyMatch)
    {
        getNodeIds(root);
        this.anyMatch = anyMatch;
        path = root.getRootPath();
    }

    // Return a list of the activated nodes' IDs that are within this node's subtree
    private void getNodeIds(TagNode tag)
    {
        if (tag.isExcluded())
            excludeIds.add(tag.getId());
        else if (tag.isActive())
            includeIds.add(tag.getId());

        for (TagNode child : tag.getChildren(true))
            getNodeIds(child);
    }
}
