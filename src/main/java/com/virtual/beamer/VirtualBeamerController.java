package com.virtual.beamer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.event.ActionEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;


public class VirtualBeamerController {
    private File[] slides;
    private int currentSlide = 0;

    @FXML
    private Button nextSlideButton;

    @FXML
    private Button previousSlideButton;

    @FXML
    private ImageView slide;

    @FXML
    void load_presentation(ActionEvent event) throws FileNotFoundException {
        currentSlide = 0;

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(new Stage());

        slides = directory.listFiles();

        Image image = new Image(new FileInputStream(slides[currentSlide]));
        slide.setImage(image);

        if (slides.length <= 1) {
            nextSlideButton.setDisable(true);
            previousSlideButton.setDisable(true);
        } else {
            nextSlideButton.setDisable(false);
        }
    }

    @FXML
    void nextSlide(ActionEvent event) throws FileNotFoundException {
        currentSlide++;
        Image image = new Image(new FileInputStream(slides[currentSlide]));
        slide.setImage(image);
        previousSlideButton.setDisable(false);

        if (currentSlide + 1 == slides.length) {
            nextSlideButton.setDisable(true);
        }
    }

    @FXML
    void previousSlide(ActionEvent event) throws FileNotFoundException {
        currentSlide--;
        Image image = new Image(new FileInputStream(slides[currentSlide]));
        slide.setImage(image);
        nextSlideButton.setDisable(false);

        if (currentSlide == 0) {
            previousSlideButton.setDisable(true);
        }
    }
}

