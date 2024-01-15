package com.example.tagger;

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

    public TagNode(final TagNode PARENT, final String TAG)
    {
        this.PARENT = PARENT;
        NODE = TAG;
        TAG_CHAIN = (PARENT != null) ? String.format("%s->%s", PARENT.getTagChain(), NODE) : NODE;
    }
}
