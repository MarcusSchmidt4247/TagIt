/* TagIt
 * SearchCriteria.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

import java.util.Vector;

public class SearchCriteria
{
    public enum SortMethod
    {
        NAME ("Alphabetically"),
        AGE("Oldest to newest"),
        IMPORT ("Import order"),
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

    private final boolean anyMatch;
    public boolean isAnyMatch() { return anyMatch; }

    private final boolean excluding;

    public SearchCriteria(TagNode root, boolean anyMatch, boolean excluding, SortMethod sortMethod)
    {
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

        // Recurse only if this tag has fetched its children (if not, they can't be active)
        if (tag.fetchedChildren())
        {
            for (TagNode child : tag.getChildren())
                getNodeIds(child);
        }
    }
}
