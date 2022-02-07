package com.virtual.beamer.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;

import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ResourceBundle;


public class PresentationViewController implements Initializable {
    private File[] slides;
    private int currentSlide = 0;
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
    void exitSession() {
        presentationStatus.setVisible(false);
        slidePane.setBackground(slidePaneDefaultBackground);
        nextSlideButton.setDisable(true);
        previousSlideButton.setDisable(true);
//        TODO: Implement when the default joining session view is added
    }

    private void updatePresentationStatus() {
        presentationStatus.setText((currentSlide + 1) + " / " + slides.length);
        presentationStatus.setVisible(true);
    }

    private void setSlide() throws FileNotFoundException {
        Image slide = new Image(new FileInputStream(slides[currentSlide]),
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
    void loadPresentation() throws FileNotFoundException {
        currentSlide = 0;

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(new Stage());

        slides = directory.listFiles();

        try {
            assert slides != null;
            if (slides.length <= 1) {
                nextSlideButton.setDisable(true);
                previousSlideButton.setDisable(true);
            } else {
                setSlide();
                nextSlideButton.setDisable(false);
            }
        } catch (NullPointerException e) {
            System.out.println("Empty directory");
//            TODO: Implement error handling
        }
    }

    @FXML
    void nextSlide() throws FileNotFoundException {
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

