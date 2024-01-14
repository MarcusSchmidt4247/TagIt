module com.example.tagger {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    opens com.example.tagger to javafx.fxml;
    exports com.example.tagger;
}