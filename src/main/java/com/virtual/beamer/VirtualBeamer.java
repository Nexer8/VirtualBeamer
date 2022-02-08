package com.virtual.beamer;

import javafx.application.Application;
import javafx.application.Platform;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

import static com.virtual.beamer.constants.AppConstants.*;

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

        stage.setOnCloseRequest(e -> Platform.exit());
    }

    public static void main(String[] args) {
        launch();
    }
}