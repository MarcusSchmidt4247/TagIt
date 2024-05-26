/* TagIt
 * FileTypes.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

public class FileTypes
{
    public enum Type { TEXT, IMAGE, VIDEO, UNSUPPORTED }

    public static Type getType(String filename)
    {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1)
        {
            String extension = filename.substring(dotIndex).toLowerCase();
            if (extension.matches("[.](txt|docx)$"))
                return Type.TEXT;
            else if (extension.matches("[.](jpe?g|png)$"))
                return Type.IMAGE;
            else if (extension.matches("[.](mp[34])$"))
                return Type.VIDEO;
        }

        return Type.UNSUPPORTED;
    }

    public static boolean isSupported(String filename) { return getType(filename) != Type.UNSUPPORTED; }
}
