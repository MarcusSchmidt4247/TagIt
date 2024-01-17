package com.example.tagger;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.Vector;

public class ImporterModel
{
    private final SimpleStringProperty IMPORT_PATH = new SimpleStringProperty();
    public SimpleStringProperty getPath() { return IMPORT_PATH; }

    private final ObservableList<File> IMPORT_FILES;
    public ObservableList<File> getFiles() { return IMPORT_FILES; }

    private final Vector<TagNode> APPLIED_TAGS;
    public Vector<TagNode> getAppliedTags() { return APPLIED_TAGS; }

    public int importIndex = -1;

    public ImporterModel()
    {
        IMPORT_FILES = FXCollections.observableArrayList();
        APPLIED_TAGS = new Vector<>();
    }
}
