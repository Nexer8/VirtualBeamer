package com.virtualbeamer.controllers;

import com.virtualbeamer.services.MainService;
import javafx.beans.binding.Bindings;
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

import static com.virtualbeamer.constants.AppConstants.UserType;
import static com.virtualbeamer.constants.AppConstants.UserType.*;

public class InitialViewController implements Initializable {
    private MainService user;

    @FXML
    private Button createButton;

    @FXML
    private ListView<String> ongoingSessions;

    @FXML
    private TextField sessionNameEditTextField;

    @FXML
    private TextField sessionNameDisplayTextField;

    @FXML
    private Button joinButton;

    @FXML
    private TextField usernameEditTextField;

    private void goToPresentationView(MouseEvent mouseEvent, UserType userType) throws IOException {
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/presentation_view.fxml"));

        stage.getScene().setRoot(fxmlLoader.load());

        user.setUserType(userType);
    }

    @FXML
    public void joinSession(MouseEvent mouseEvent) throws IOException {
        user.setUserType(VIEWER);
        user.setUsername(usernameEditTextField.getText());
        MainService.stopSendingPeriodicalHELLO();
        String session = ongoingSessions.getSelectionModel().getSelectedItem().substring(0,
                ongoingSessions.getSelectionModel().getSelectedItem().indexOf(":"));
        MainService.getInstance().joinSession(session);
        goToPresentationView(mouseEvent, VIEWER);
    }

    @FXML
    public void createSession(MouseEvent mouseEvent) throws IOException {
        user.setUsername(usernameEditTextField.getText());
        user.createSession(sessionNameEditTextField.getText());
        MainService.stopSendingPeriodicalHELLO();
        goToPresentationView(mouseEvent, PRESENTER);
    }

    public void displaySelectedSession() {
        String session = ongoingSessions.getSelectionModel().getSelectedItem();

        if (session == null || session.isEmpty()) {
            sessionNameDisplayTextField.setText("");
        } else {
            sessionNameDisplayTextField.setText(session);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        usernameEditTextField.setFocusTraversable(false);
        sessionNameEditTextField.setFocusTraversable(false);

        createButton.disableProperty()
                .bind(Bindings.createBooleanBinding(() -> usernameEditTextField.getText().isEmpty()
                                || sessionNameEditTextField.getText().isEmpty(),
                        usernameEditTextField.textProperty(),
                        sessionNameEditTextField.textProperty()));

        joinButton.disableProperty()
                .bind(Bindings.createBooleanBinding(() -> usernameEditTextField.getText().isEmpty()
                                || ongoingSessions.getSelectionModel().getSelectedItem() == null,
                        usernameEditTextField.textProperty(),
                        ongoingSessions.getSelectionModel().selectedItemProperty()));

        try {
            user = MainService.getInstance();
            ongoingSessions.setItems(user.getGroupSessionsInfo());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
