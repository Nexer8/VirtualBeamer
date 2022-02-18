package com.virtual.beamer.controllers;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static com.virtual.beamer.constants.AppConstants.UserType;
import static com.virtual.beamer.constants.AppConstants.UserType.VIEWER;


public class PresentationViewController implements Initializable {
    private File[] slides;
    private int currentSlide = 0;
    private Background slidePaneDefaultBackground;
    private UserType userType;

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
        presentationStatus.setText((currentSlide + 1) + " / " + slides.length);
        presentationStatus.setVisible(true);
    }

    private void setSlide() throws FileNotFoundException {
        Image slide = new Image(new FileInputStream(
                slides[currentSlide]), slidePane.getWidth(), slidePane.getHeight(), true, true);

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

    public void setUserType(UserType userType) {
        this.userType = userType;

        if (userType == VIEWER) {
            loadPresentationButton.setVisible(false);
            nextSlideButton.setDisable(true);
            previousSlideButton.setDisable(true);
            progressIndicator.setVisible(true);
        }
    }

    @FXML
    public void loadPresentation() throws FileNotFoundException {
        currentSlide = 0;

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(new Stage());

        try {
            slides = directory.listFiles();
            assert slides != null;

            if (slides.length <= 1) {
                nextSlideButton.setDisable(true);
                previousSlideButton.setDisable(true);
            } else {
                setSlide();
                nextSlideButton.setDisable(false);
            }
        } catch (NullPointerException e) {
            System.out.println("No data provided!");
//            TODO: Implement error handling
        }
    }

    @FXML
    public void nextSlide() throws FileNotFoundException {
        currentSlide++;
        setSlide();
        previousSlideButton.setDisable(false);

        if (currentSlide + 1 == slides.length) {
            nextSlideButton.setDisable(true);
        }
    }

    @FXML
    public void previousSlide() throws FileNotFoundException {
        currentSlide--;
        setSlide();
        nextSlideButton.setDisable(false);

        if (currentSlide == 0) {
            previousSlideButton.setDisable(true);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        slidePaneDefaultBackground = slidePane.getBackground();
    }
}

