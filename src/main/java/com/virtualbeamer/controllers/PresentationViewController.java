package com.virtualbeamer.controllers;

import com.virtualbeamer.models.Participant;
import com.virtualbeamer.services.MainService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;

import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

import static com.virtualbeamer.constants.AppConstants.UserType.PRESENTER;
import static com.virtualbeamer.constants.AppConstants.UserType.VIEWER;

public class PresentationViewController implements Initializable {
    private MainService user;

    private Background slidePaneDefaultBackground;

    @FXML
    private TextField presentationStatus;

    @FXML
    private Button nextSlideButton;

    @FXML
    private ComboBox<Participant> participants;

    @FXML
    private Button previousSlideButton;

    @FXML
    private Pane slidePane;

    @FXML
    private VBox progressIndicator;

    @FXML
    private Button loadPresentationButton;

    @FXML
    private Button giveControlButton;

    @FXML
    private Label presenterLabel;

    @FXML
    private void exitSession() throws IOException {
        cleanUpView();
        MainService.getInstance().stopCrashDetection();
        if (user.getUserType() == PRESENTER) {
            user.sendDeleteSession();
        } else {
            user.leaveSession();
        }
        goToInitialView();
    }

    private void goToInitialView() throws IOException {
        MainService.startSendingPeriodicalHELLO();
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

    public void closeSession() throws IOException {
        if (slidePane.getScene() != null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Presenter has finished the session!");
            alert.initOwner(slidePane.getScene().getWindow());
            alert.getDialogPane().getStylesheets().add((Objects.requireNonNull(
                    getClass().getResource("/styles/dialog.css"))).toExternalForm());
            alert.setHeaderText("Presentation ended!");
            alert.showAndWait();

            goToInitialView();
        }
    }

    public void showParticipantUnavailableAlert(String name) {
        Alert alert = new Alert(Alert.AlertType.WARNING, name + " is no longer available!");
        alert.initOwner(slidePane.getScene().getWindow());
        alert.getDialogPane().getStylesheets().add((Objects.requireNonNull(
                getClass().getResource("/styles/dialog.css"))).toExternalForm());
        alert.setHeaderText("Cannot pass control!");
        alert.showAndWait();
    }

    private void updatePresentationStatus() {
        presentationStatus.setText((user.getCurrentSlide() + 1) + " / " + user.getSlides().size());
        presentationStatus.setVisible(true);
    }

    public void setSlide() throws IOException {
        progressIndicator.setVisible(false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(user.getSlides().get(user.getCurrentSlide()), "jpeg", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        Image slide = new Image(is, slidePane.getWidth(), slidePane.getHeight(), true, true);

        BackgroundImage backgroundImage = new BackgroundImage(
                slide,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(1.0, 1.0, true, true, true, false));

        slidePane.setBackground(new Background(backgroundImage));
        updatePresentationStatus();
    }

    @FXML
    public void loadPresentation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(new Stage());

        try {
            File[] files = directory.listFiles(file -> !file.isHidden() && !file.isDirectory()
                    && (file.getName().matches(".*\\.(jpg|png|jpeg)")));
            if (Objects.requireNonNull(files).length == 0) {
                throw new IOException();
            }
            ArrayList<BufferedImage> images = new ArrayList<>();
            for (var file : files) {
                images.add(ImageIO.read(file));
            }
            user.setSlides(images.toArray(new BufferedImage[0]));
            user.setCurrentSlide(0);

            if (user.getSlides().size() <= 1) {
                nextSlideButton.setDisable(true);
                previousSlideButton.setDisable(true);
            } else {
                setSlide();
                nextSlideButton.setDisable(false);
            }

            if (!MainService.getInstance().getParticipantsNames().isEmpty()) {
                user.multicastSlides();
            }
        } catch (NullPointerException | IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No images found in the directory!");
            alert.initOwner(slidePane.getScene().getWindow());
            alert.getDialogPane().getStylesheets().add((Objects.requireNonNull(
                    getClass().getResource("/styles/dialog.css"))).toExternalForm());
            alert.setHeaderText("Nothing do display!");
            alert.showAndWait();
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

    @FXML
    public void giveControl() {
        Participant participant = participants.getValue();
        if (participant != null) {
            user.setGroupLeader(participant);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        slidePaneDefaultBackground = slidePane.getBackground();

        try {
            user = MainService.getInstance();
            participants.setItems(user.getParticipantsNames());

            if (user.getUserType() == VIEWER) {
                initializeParticipantsComboBox();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        presenterLabel.setText("Presenter: " + user.getGroupSession().getLeaderInfo());

        if (user.getUserType() == VIEWER) {
            loadPresentationButton.setVisible(false);
            nextSlideButton.setDisable(true);
            previousSlideButton.setDisable(true);
            progressIndicator.setVisible(true);
            giveControlButton.setVisible(false);
        }

        user.setPvc(this);
    }

    public void changePresenterData(String leaderInfo) {
        if (user.getUserType() == VIEWER) {
            System.out.println("I'm a VIEWER");
            loadPresentationButton.setVisible(false);
            nextSlideButton.setDisable(true);
            previousSlideButton.setDisable(true);

            if (slidePane.getBackground() == null) {
                progressIndicator.setVisible(true);
            }
            giveControlButton.setVisible(false);

            Platform.runLater(() -> participants.getSelectionModel().clearSelection());
        }

        initializeParticipantsComboBox();

        if (user.getUserType() == PRESENTER) {
            System.out.println("I'm a PRESENTER");
            loadPresentationButton.setVisible(true);
            System.out.println("Current slide: " + user.getCurrentSlide() + 1);
            if (user.getCurrentSlide() + 1 < user.getSlides().size()) {
                nextSlideButton.setDisable(false);
            }
            if (user.getCurrentSlide() > 0) {
                previousSlideButton.setDisable(false);
            }
            progressIndicator.setVisible(false);
            giveControlButton.setVisible(true);
        }

        Platform.runLater(() -> presenterLabel.setText("Presenter: " + leaderInfo));
    }

    private void initializeParticipantsComboBox() {
        participants.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(Participant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Participants");
                } else {
                    setText(String.valueOf(item));
                    setDisable(user.getUserType() == VIEWER);
                }
            }
        });
    }

    public VBox getProgressIndicator() {
        return progressIndicator;
    }
}
