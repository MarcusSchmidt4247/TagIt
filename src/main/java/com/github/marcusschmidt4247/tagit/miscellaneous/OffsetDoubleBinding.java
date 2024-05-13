/* TagIt
 * OffsetDoubleBinding.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;

// A class that binds two Double properties together with an offset
public class OffsetDoubleBinding extends DoubleBinding
{
    private final ObservableDoubleValue PROPERTY;
    private final double OFFSET;
    private double prevResult;

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
