package com.example.tagger;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFilter implements FilenameFilter
{
    @Override
    public boolean accept(File dir, String name) { return validExtension(name); }

    public static boolean validExtension(String name) { return name.toLowerCase().matches(".+[.](jpe?g|png|mp[34])$"); }
}
