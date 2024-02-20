package com.example.tagger.miscellaneous;

import java.util.Vector;

public class SearchCriteria
{
    public enum SortMethod
    {
        NAME ("Alphabetically"),
        OLD_NEW ("Old to new"),
        NEW_OLD ("New to old"),
        RANDOM ("Random");

        public final String description;
        SortMethod(String description) { this.description = description; }
    }
    private final SortMethod sortMethod;
    public SortMethod getSortMethod() { return sortMethod; }

    private final Vector<Integer> includeAny = new Vector<>();
    public Vector<Integer> getIncludeAny() { return includeAny; }

    private final Vector<TagNode> includeAll = new Vector<>();
    public Vector<TagNode> getIncludeAll() { return includeAll; }

    private final Vector<Integer> excludeIds = new Vector<>();
    public Vector<Integer> getExcludeIds() { return excludeIds; }

    private final String path;
    public String getPath() { return path; }

    private final boolean anyMatch;
    public boolean isAnyMatch() { return anyMatch; }

    private final boolean excluding;

    public SearchCriteria(TagNode root, boolean anyMatch, boolean excluding, SortMethod sortMethod)
    {
        path = root.getRootPath();
        this.anyMatch = anyMatch;
        this.excluding = excluding;
        this.sortMethod = sortMethod;
        getNodeIds(root);
    }

    // Return a list of the activated nodes' IDs that are within this node's subtree
    private void getNodeIds(TagNode tag)
    {
        if (excluding && tag.isExcluded())
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
