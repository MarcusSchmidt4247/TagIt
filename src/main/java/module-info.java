module com.example.tagger {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires java.sql;

    opens com.example.tagger to javafx.fxml;
    exports com.example.tagger;
}