module com.github.marcusschmidt4247.tagit {
    requires java.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires java.sql;

    opens com.github.marcusschmidt4247.tagit to javafx.fxml;
    exports com.github.marcusschmidt4247.tagit;
    exports com.github.marcusschmidt4247.tagit.controllers;
    opens com.github.marcusschmidt4247.tagit.controllers to javafx.fxml;
    exports com.github.marcusschmidt4247.tagit.models;
    opens com.github.marcusschmidt4247.tagit.models to javafx.fxml;
    exports com.github.marcusschmidt4247.tagit.gui;
    opens com.github.marcusschmidt4247.tagit.gui to javafx.fxml;
    exports com.github.marcusschmidt4247.tagit.miscellaneous;
    opens com.github.marcusschmidt4247.tagit.miscellaneous to javafx.fxml;
}