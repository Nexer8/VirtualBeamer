package com.virtual.beamer.controllers;

import com.virtual.beamer.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static com.virtual.beamer.constants.AppConstants.UserType.VIEWER;


public class PresentationViewController implements Initializable {
    private User user;

    private Background slidePaneDefaultBackground;

    @FXML
    private TextField presentationStatus;

    @FXML
    private Button nextSlideButton;

    @FXML
    private Button previousSlideButton;

    @FXML
    private Pane slidePane;

    @FXML
    private VBox progressIndicator;

    @FXML
    private MenuItem loadPresentationButton;

    @FXML
    private void exitSession() throws IOException {
//        TODO: implement leader election
//        if (userType == PRESENTER) {}
        cleanUpView();

        Stage stage = (Stage) slidePane.getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/initial_view.fxml"));

        stage.getScene().setRoot(fxmlLoader.load());
    }

    private void cleanUpView() {
        presentationStatus.setVisible(false);
        slidePane.setBackground(slidePaneDefaultBackground);
        nextSlideButton.setDisable(true);
        previousSlideButton.setDisable(true);
    }

    private void updatePresentationStatus() {
        presentationStatus.setText((user.getCurrentSlide() + 1) + " / " + user.getSlides().size());
        presentationStatus.setVisible(true);
    }

    public void setSlide() throws IOException {
        Image slide = new Image(new FileInputStream(
                user.getSlides().get(user.getCurrentSlide())),
                slidePane.getWidth(), slidePane.getHeight(), true, true);

        BackgroundImage backgroundImage = new BackgroundImage(
                slide,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, true, false)
        );

        slidePane.setBackground(new Background(backgroundImage));
        updatePresentationStatus();
    }

    @FXML
    public void loadPresentation() throws IOException {
        user.setCurrentSlide(0);

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(new Stage());
        File[] files = directory.listFiles();

        try {
            user.setSlides(files);
            assert user.getSlides() != null;
            user.multicastSlides();
            if (user.getSlides().size() <= 1) {
                nextSlideButton.setDisable(true);
                previousSlideButton.setDisable(true);
            } else {
                setSlide();
                nextSlideButton.setDisable(false);
            }
        } catch (NullPointerException | IOException e) {
            System.out.println("No data provided!");
//            TODO: Implement error handling
        }
    }

    @FXML
    public void nextSlide() throws IOException {
        user.multicastNextSlide();
        setSlide();
        previousSlideButton.setDisable(false);

        if (user.getCurrentSlide() + 1 == user.getSlides().size()) {
            nextSlideButton.setDisable(true);
        }
    }

    @FXML
    public void previousSlide() throws IOException {
        user.multicastPreviousSlide();
        setSlide();

        nextSlideButton.setDisable(false);

        if (user.getCurrentSlide() == 0) {
            previousSlideButton.setDisable(true);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        slidePaneDefaultBackground = slidePane.getBackground();

        try {
            user = User.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (user.getUserType() == VIEWER) {
            loadPresentationButton.setVisible(false);
            nextSlideButton.setDisable(true);
            previousSlideButton.setDisable(true);
            progressIndicator.setVisible(true);
        }

        user.setPvc(this);
    }

    public VBox getProgressIndicator() {
        return progressIndicator;
    }
}

