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

    /**
     * Isolates the extension in a file name.
     * @param filename the name of a file
     * @return the extension in lowercase; <code>null</code> if it does not exist
     */
    public static String getExtension(String filename)
    {
        int dotIndex = filename.toLowerCase().lastIndexOf('.');
        if (dotIndex != -1)
            return filename.substring(dotIndex);
        else
            return null;
    }

    /**
     * Matches a file name to the type of file it represents. Multiple file extensions can belong to the same type (such as
     * <code>.txt</code> and <code>.docx</code> both belonging to the <code>TEXT</code> category).
     * @param filename the name of the file
     * @return the file's category
     */
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
                String extension = getExtension(name);
                if (extension != null)
                {
                    if (extension.matches("[.](jpe?g|png)$"))
                        return Type.IMAGE;
                    else if (extension.matches("[.](mp[34])$"))
                        return Type.VIDEO;
                }
            }
        }

        return Type.UNSUPPORTED;
    }

    public static boolean isSupported(String filename) { return getType(filename) != Type.UNSUPPORTED; }
}
