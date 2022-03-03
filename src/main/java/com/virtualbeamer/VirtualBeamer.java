package com.virtualbeamer;

import com.virtualbeamer.utils.ServiceRegistrar;
import javafx.application.Application;
import javafx.application.Platform;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

import static com.virtualbeamer.constants.AppConstants.*;

public class VirtualBeamer extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(MIN_WINDOW_HEIGHT);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/initial_view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);

        stage.getIcons().add(new Image(String.valueOf(getClass().getResource("/icons/app_icon.png"))));
        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) throws IOException {
        ServiceRegistrar.registerServices();
        launch();
    }
}