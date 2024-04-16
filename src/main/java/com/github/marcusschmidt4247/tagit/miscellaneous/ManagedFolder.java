/* TagIt
 * ManagedFolder.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

import com.github.marcusschmidt4247.tagit.IOManager;
import javafx.beans.property.*;

public class ManagedFolder
{
    private final StringProperty name;
    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    // 'location' is the path to the managed folder's parent directory, NOT to the folder itself
    private final StringProperty location;
    public String getLocation() { return location.get(); }
    public StringProperty locationProperty() { return location; }
    public void setLocation(String location) { this.location.set(location); }

    public String getFullPath() { return IOManager.formatPath(location.get(), name.get()); }

    private final BooleanProperty mainFolder;
    public boolean isMainFolder() { return mainFolder.get(); }
    public void setMainFolder(boolean mainFolder) { this.mainFolder.set(mainFolder); }

    // A string representation of the boolean 'mainFolder' property
    SimpleStringProperty mainFolderStringProperty;
    public StringProperty mainFolderProperty() { return mainFolderStringProperty; }

    public ManagedFolder(String name, String location, boolean mainFolder)
    {
        this.name = new SimpleStringProperty();
        this.name.setValue((name != null) ? name : "Main");

        this.location = new SimpleStringProperty();
        this.location.set((location != null) ? location : "");

        this.mainFolder = new SimpleBooleanProperty();
        this.mainFolder.set(mainFolder);

        mainFolderStringProperty = new SimpleStringProperty();
        mainFolderStringProperty.set(getMainFolderString(mainFolder));
        this.mainFolder.addListener((observableValue, aBoolean, t1) -> mainFolderStringProperty.set(getMainFolderString(observableValue.getValue())));
    }

    private String getMainFolderString(boolean main) { return main ? "yes" : ""; }
}
