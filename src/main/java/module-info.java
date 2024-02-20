module com.example.tagger {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires java.sql;

    opens com.example.tagger to javafx.fxml;
    exports com.example.tagger;
    exports com.example.tagger.controllers;
    opens com.example.tagger.controllers to javafx.fxml;
    exports com.example.tagger.models;
    opens com.example.tagger.models to javafx.fxml;
    exports com.example.tagger.gui;
    opens com.example.tagger.gui to javafx.fxml;
    exports com.example.tagger.miscellaneous;
    opens com.example.tagger.miscellaneous to javafx.fxml;
}