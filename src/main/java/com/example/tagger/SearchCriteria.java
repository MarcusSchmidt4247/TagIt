package com.example.tagger;

import java.util.Vector;

public class SearchCriteria
{
    private final Vector<Integer> includeAny = new Vector<>();
    public Vector<Integer> getIncludeAny() { return includeAny; }

    private final Vector<TagNode> includeAll = new Vector<>();
    public Vector<TagNode> getIncludeAll() { return includeAll; }

    private final Vector<Integer> excludeIds = new Vector<>();
    public Vector<Integer> getExcludeIds() { return excludeIds; }

    private final boolean anyMatch;
    public boolean isAnyMatch() { return anyMatch; }

    private final String path;
    public String getPath() { return path; }

    public SearchCriteria(TagNode root, boolean anyMatch)
    {
        this.anyMatch = anyMatch;
        path = root.getRootPath();
        getNodeIds(root);
    }

    // Return a list of the activated nodes' IDs that are within this node's subtree
    private void getNodeIds(TagNode tag)
    {
        if (tag.isExcluded())
            excludeIds.add(tag.getId());
        else if (tag.isActive())
        {
            if (anyMatch)
                includeAny.add(tag.getId());
            else if (tag.isSelfActivated())
                includeAll.add(tag);
        }

        for (TagNode child : tag.getChildren(true))
            getNodeIds(child);
    }
}
