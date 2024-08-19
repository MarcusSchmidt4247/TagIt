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

    private final StringProperty location;
    /**
     * Gets the path to this directory's parent. Generally used for getting this directory's location - not ideal
     * for accessing files within it (use <code>getFullPath()</code> for that).
     * @return an absolute file path
     */
    public String getLocation() { return location.get(); }
    public StringProperty locationProperty() { return location; }
    public void setLocation(String location) { this.location.set(location); }

    /**
     * Gets the path to this directory. Generally used for accessing files within this directory - not ideal
     * for getting this directory's location (use <code>getLocation()</code> for that).
     * @return an absolute file path
     */
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

    /**
     * Class constructor that defaults to <code>null</code> values.
     */
    public ManagedFolder() { this(-1, null, null, null); }

    /**
     * Class constructor for new folders without a database-assigned ID.
     * @param name the title of this folder
     * @param location the absolute path to this folder's parent directory
     * @param mainFolder <code>true</code> if this folder is the user's chosen default; <code>false</code> otherwise
     */
    public ManagedFolder(String name, String location, boolean mainFolder) { this(-1, name, location, mainFolder); }

    /**
     * Class constructor for existing folders.
     * @param id the database-assigned ID
     * @param name the title of this folder
     * @param location the absolute path to this folder's parent directory
     * @param mainFolder <code>true</code> if this folder is the user's chosen default; <code>false</code> otherwise
     */
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

    /**
     * Updates this folder's properties with the values in <code>delta</code>.
     * @param delta the set of updated values; any property that is <code>null</code> will not be updated
     */
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
