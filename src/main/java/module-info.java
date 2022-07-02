module com.virtualbeamer {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.apache.commons.collections4;

    opens com.virtualbeamer to javafx.fxml;

    exports com.virtualbeamer;
    exports com.virtualbeamer.controllers;

    opens com.virtualbeamer.controllers to javafx.fxml;
}