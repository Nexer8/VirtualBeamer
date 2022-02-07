module com.example.virtualbeamer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.virtual.beamer to javafx.fxml;
    exports com.virtual.beamer;
    exports com.virtual.beamer.controllers;
    opens com.virtual.beamer.controllers to javafx.fxml;
}