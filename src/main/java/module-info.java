module com.github.marcusschmidt4247.tagit {
    requires java.base;
    requires java.sql;
    requires jdk.unsupported;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires jakarta.xml.bind;
    requires org.docx4j.core;
    requires org.docx4j.JAXB_ReferenceImpl;

    exports com.github.marcusschmidt4247.tagit;
    exports com.github.marcusschmidt4247.tagit.controllers;
    exports com.github.marcusschmidt4247.tagit.gui;
    exports com.github.marcusschmidt4247.tagit.miscellaneous;
    exports com.github.marcusschmidt4247.tagit.models;

    opens com.github.marcusschmidt4247.tagit to javafx.fxml;
    opens com.github.marcusschmidt4247.tagit.controllers to javafx.fxml;
    opens com.github.marcusschmidt4247.tagit.gui to javafx.fxml;
    opens com.github.marcusschmidt4247.tagit.miscellaneous to javafx.fxml;
    opens com.github.marcusschmidt4247.tagit.models to javafx.fxml;
}