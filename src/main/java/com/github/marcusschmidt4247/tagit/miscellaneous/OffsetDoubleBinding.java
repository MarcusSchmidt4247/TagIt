/* TagIt
 * OffsetDoubleBinding.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;

/**
 * Binds to a <code>Double</code> property with some difference in value.
 */
public class OffsetDoubleBinding extends DoubleBinding
{
    private final ObservableDoubleValue PROPERTY;
    private final double OFFSET;
    private double prevResult;

    /**
     * Class constructor. Creates a <code>DoubleBinding</code> that is always <code>OFFSET</code> less than <code>property</code>'s value.
     * @param property the value to be offset as it changes
     * @param OFFSET the value difference between this and <code>property</code>
     */
    public OffsetDoubleBinding(ObservableDoubleValue property, final Double OFFSET)
    {
        PROPERTY = property;
        bind(PROPERTY);
        this.OFFSET = OFFSET;
    }

    @Override
    protected double computeValue()
    {
        /* If the bound value is closer to this value than a quarter of the offset, then it must be decreasing
         * quickly and should be given more room to shrink per frame */
        double nextOffset = OFFSET;
        if (PROPERTY.doubleValue() < prevResult + (OFFSET / 4))
            nextOffset *= 3;
        return (prevResult = PROPERTY.doubleValue() - nextOffset);
    }
}
