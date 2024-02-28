/* TagIt
 * ExtensionFilter.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFilter implements FilenameFilter
{
    @Override
    public boolean accept(File dir, String name) { return validExtension(name); }

    public static boolean validExtension(String name) { return name.toLowerCase().matches(".+[.](jpe?g|png|mp[34])$"); }
}
