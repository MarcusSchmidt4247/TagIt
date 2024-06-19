/* TagIt
 * FileTypes.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

public class FileTypes
{
    public enum Type
    {
        TEXT ("Text"),
        IMAGE("Image"),
        VIDEO ("Video"),
        UNSUPPORTED ("N/A");

        public final String description;
        Type(String description) { this.description = description; }

        public static Type instanceOf(String descriptor)
        {
            for (Type type : Type.values())
            {
                if (type.description.equals(descriptor))
                    return type;
            }
            return UNSUPPORTED;
        }
    }

    public static Type getType(String filename)
    {
        if (filename != null)
        {
            // Check the file's name for the text file type (cannot begin with "~$" and must end in a valid extension)
            String name = filename.toLowerCase();
            if (name.matches("^(?!~[$]).+[.](txt|docx)$"))
                return Type.TEXT;
            else
            {
                // Check only the file's extension for the other file types
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex != -1)
                {
                    String extension = name.substring(dotIndex);
                    if (extension.matches("[.](jpe?g|png)$"))
                        return Type.IMAGE;
                    else if (extension.matches("[.](mp[34])$"))
                        return Type.VIDEO;
                }
                else
                    System.out.printf("FileTypes.getType: File \"%s\" has no extension\n", filename);
            }
        }

        return Type.UNSUPPORTED;
    }

    public static boolean isSupported(String filename) { return getType(filename) != Type.UNSUPPORTED; }
}
