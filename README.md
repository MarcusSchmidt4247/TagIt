# TagIt

## About

File organization can be the key or bottleneck to productivity. Too little organization allows for important files to be lost, but too much organization limits the view of your files to only a tiny slice at a time.

*TagIt* is a desktop application in development for macOS and Windows that aims to solve this problem by providing a flexible, tag-based system to see as big or small of a view of your files as you want.[^1] The application manages its own directory of imported files, each of which can be tagged with as many user-created identifiers as desired. Later, select the tags that you do and don't want to see, and *TagIt* will display the files that match your criteria.

The goal of this project is to provide a free tool that is equally suitable for managing personal and work files. The following video is a small example of how photos from a variety of categories can be quickly filtered to exactly what you want to see.

<p align="center">
<video>
    <source src="assets/2024_2_26_tagging_demo.mp4" type="video/mp4"/>
</video>

*Example photos courtesy of [Matthew Henry](https://www.shopify.com/stock-photos/@matthew_henry)*
</p>

*TagIt* is written in Java using [JavaFX](https://openjfx.io), [ControlsFX](https://github.com/controlsfx/controlsfx), and [ikonli](https://github.com/kordamp/ikonli) for the GUI; [SQLite](https://www.sqlite.org) for the database; and with [IntelliJ IDEA](https://www.jetbrains.com/idea) as the development environment.

## Installation

Download the platform-appropriate JAR file or executable from the latest release on the [releases page](https://github.com/MarcusSchmidt4247/TagIt/releases). To run a JAR file, a [Java runtime environment](https://www.java.com/en/) or [development kit](https://www.oracle.com/java/technologies/downloads/) (version 21+ recommended) is also required.

When *TagIt* runs for the first time, it will automatically create a directory for its own use (managing the files you import) in the user's application folder (`/Users/[user]/Applications` on macOS and `C:\Users\[user]\AppData\Local\Programs` on Windows).

## How to Use

To run from a JAR file, open the command line, navigate to the directory containing `tagit.jar` and its dependencies, and execute `java -jar tagit.jar`.

The first window that opens when you run *TagIt* is the File Viewer, where you choose your search parameters and then view, edit, or delete the files that are returned by the search. Use the left and right arrow keys to move through the list of returned files.

The File Importer window is where you can create, edit, or delete tags and then apply them to files that you want to import. After you import a file, you can search for it in the File Viewer window. Open this window by selecting the `Files` dropdown menu and then `Import New Files`.

For more detailed descriptions of how to utilize all the features in these windows, read the corresponding sections below.

### File Viewer

The controls to define your search parameters are on the left side of the window in an expandable pane. It is also split vertically so that you can hide or show the basic and advanced search controls.
- The most important search control is the `Included Tags` tree, which shows all of your tags as nested checkboxes that you can select to start searching for those tags. You can include as many tags in the search as desired. When a tag has other tags nested within it (a parent tag and its children), selecting a parent will also include its children in the search.
- There are several more advanced controls in the bottom half of the control pane that allow you to refine your search.
  - The `Sort Method` selector controls the order in which you see your files.
    - Alphabetically: Order by file name (case-insensitive).
    - Oldest to newest: Order by file creation date.
    - Import order: Order from the files that were imported first to last.
    - Random: Shuffle the file order each time the search parameters are changed.
  - The `Search Criteria` selector controls how files are compared to the list of tags included in a search, which affects what files you see in the result.
    - Any matching tag: Files will be added to the search result if they have been tagged with any of the included tags or their children.
    - All matching tags: Files will be added to the search result only if they have been tagged with every included tag. When an included tag is a parent, a file can be tagged with *any* of its children to satisfy the requirement for the parent.
  - The `Excluded Tags` tree is like the `Included Tags` tree but excludes all of the tags selected in it from the search result. It is hidden by default and can be revealed by clicking the arrow button or selecting the checkbox next to its label at the bottom of the control pane.
    - Important: Tags selected in this tree will only be excluded from the search result when the checkbox next to its label is selected.

To edit a file's name, change its tags, or delete it, select the `Files` dropdown menu and then `Toggle File Editing`. A second expandable control pane will appear on the right side of the window. The controls will be disabled unless a search has returned at least one file, in which case they will edit the file currently being displayed.
- Important: The tag tree in this control will be automatically updated to show the current file's tags. Selecting and deselecting tags in this tree changes how the current file is tagged. It *does not* have any effect on the current search parameters.

### File Importer

To start importing files, choose a source folder by clicking the button in the top-left corner. The preview on the left side of the window will automatically update to show the next file to import. Incompatible files will not be shown.

Create or select tags in the `Tag Selector` on the right side of the window that you want to be applied to the current file. Right-click within the control to see options to create or edit tags.

The `Import` button at the bottom-right of the window copies the current file into *TagIt*'s directory and saves the tags applied to it. At least one tag has to be applied to the file in order to import it. The file can be viewed again by searching for any of its tags from the main window.

## Future

Development for this project is ongoing. Planned features include:
- Support for a wider variety of file types
- Support for multiple tagged directories with customizable locations
- More options for how to manage tags and files
- Saving and reloading search parameters
- Exporting search results
- Improved logging and error reports
- Linux support

## Credit

The technique used for mixing TreeItem and CheckBoxTreeItem elements in a TreeView is courtesy of [Simon Lissack](https://blog.idrsolutions.com/mixed-treeview-nodes-javafx/).

## Licenses

This software is licensed under GNU GPLv3 (see LICENSE.txt) and uses third party libraries distributed under their own terms (see LICENSE-3RD-PARTY.txt).

[^1]: Supported file types are currently limited to JPG, PNG, MP3, and MP4.