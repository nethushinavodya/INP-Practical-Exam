module INP.Final.Practical {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    exports org.example.controller;
    exports org.example to javafx.graphics;
    opens org.example.controller to javafx.fxml;
}