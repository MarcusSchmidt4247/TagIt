/* TagIt
 * ManagedFolder.java
 * Copyright (C) 2024  Marcus Schmidt
 * SPDX-License-Identifier: GPL-3.0-or-later */

package com.github.marcusschmidt4247.tagit.miscellaneous;

import com.github.marcusschmidt4247.tagit.IOManager;
import javafx.beans.property.*;

public class ManagedFolder
{
    private int id;
    public int getId() { return id; }
    public void setId(int id)
    {
        if (this.id == -1)
            this.id = id;
    }

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

    private BooleanProperty mainFolder = null;
    public Boolean isMainFolder() { return (mainFolder != null) ? mainFolder.get() : null; }
    public void setMainFolder(boolean mainFolder)
    {
        if (this.mainFolder == null)
        {
            this.mainFolder = new SimpleBooleanProperty();

            mainFolderStringProperty = new SimpleStringProperty();
            mainFolderStringProperty.set(getMainFolderString(mainFolder));
            this.mainFolder.addListener((observableValue, aBoolean, t1) -> mainFolderStringProperty.set(getMainFolderString(observableValue.getValue())));
        }

        this.mainFolder.set(mainFolder);
    }

    // A string representation of the boolean 'mainFolder' property
    private SimpleStringProperty mainFolderStringProperty = null;
    public StringProperty mainFolderProperty() { return mainFolderStringProperty; }

    private final BooleanProperty deleted;
    public BooleanProperty deletedProperty() { return deleted; }

    public ManagedFolder() { this(-1, null, null, null); }

    public ManagedFolder(String name, String location, boolean mainFolder) { this(-1, name, location, mainFolder); }

    public ManagedFolder(int id, String name, String location, Boolean mainFolder)
    {
        this.id = id;

        this.name = new SimpleStringProperty();
        this.name.setValue(name);

        this.location = new SimpleStringProperty();
        this.location.set(location);

        if (mainFolder != null)
            setMainFolder(mainFolder);

        deleted = new SimpleBooleanProperty();
        deleted.set(false);
    }

    public boolean equals(ManagedFolder other) { return id != -1 && id == other.getId(); }

    public void set(ManagedFolder delta)
    {
        if (delta.getName() != null)
            name.set(delta.getName());
        if (delta.getLocation() != null)
            location.set(delta.getLocation());
        if (delta.isMainFolder() != null)
            mainFolder.set(delta.isMainFolder());
    }

    public void delete() { deleted.set(true); }

    private String getMainFolderString(boolean main) { return main ? "yes" : ""; }
}
