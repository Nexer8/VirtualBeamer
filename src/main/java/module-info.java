module com.example.virtualbeamer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens com.virtualbeamer to javafx.fxml;
    exports com.virtualbeamer;
    exports com.virtualbeamer.controllers;
    opens com.virtualbeamer.controllers to javafx.fxml;
}