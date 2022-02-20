package com.virtual.beamer.controllers;

import com.virtual.beamer.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


import static com.virtual.beamer.constants.AppConstants.UserType;
import static com.virtual.beamer.constants.AppConstants.UserType.*;


public class InitialViewController implements Initializable {
    ObservableList<String> list = FXCollections.observableArrayList();

    @FXML
    public ListView<String> ongoingSessions;

    @FXML
    public TextField sessionNameEditTextField;

    @FXML
    public TextField sessionNameDisplayTextField;

    @FXML
    public Button joinButton;


    private void goToPresentationView(MouseEvent mouseEvent, UserType userType) throws IOException {
        User.getInstance().establishSession();

        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/presentation_view.fxml"));

        stage.getScene().setRoot(fxmlLoader.load());

        PresentationViewController controller = fxmlLoader.getController();
        controller.setUserType(userType);
    }

    @FXML
    public void joinSession(MouseEvent mouseEvent) throws IOException {
        goToPresentationView(mouseEvent, VIEWER);
    }

    @FXML
    public void createSession(MouseEvent mouseEvent) throws IOException {
        if (!sessionNameEditTextField.getText().isEmpty()) {
            goToPresentationView(mouseEvent, PRESENTER);
        } else {
            sessionNameEditTextField.requestFocus();
        }
    }

    public void displaySelectedSession() {
        String session = ongoingSessions.getSelectionModel().getSelectedItem();

        if (session == null || session.isEmpty()) {
            sessionNameDisplayTextField.setText("");
        } else {
            sessionNameDisplayTextField.setText(session);
            joinButton.setDisable(false);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ongoingSessions.getItems().addAll(list);
        sessionNameEditTextField.setFocusTraversable(false);
    }
}
