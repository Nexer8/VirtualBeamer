package com.virtual.beamer.controllers;

import com.virtual.beamer.RuntimeTest;
import com.virtual.beamer.Session;
import com.virtual.beamer.User;
import com.virtual.beamer.VirtualBeamer;
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
import java.util.LinkedList;
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
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/presentation_view.fxml"));

        stage.getScene().setRoot(fxmlLoader.load());

        PresentationViewController controller = fxmlLoader.getController();
        controller.setUserType(userType);
    }

    @FXML
    public void joinSession(MouseEvent mouseEvent) throws IOException {
        // to access the session: VirtualBeamer.getUser().getActiveSession().get(i) where i is the current index in the list session
        goToPresentationView(mouseEvent, VIEWER);
    }

    @FXML
    public void createSession(MouseEvent mouseEvent) throws IOException {
        if (!sessionNameEditTextField.getText().isEmpty()) {
            goToPresentationView(mouseEvent, PRESENTER);
            VirtualBeamer.getUser().addSession(new Session(sessionNameEditTextField.getText()));
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
        VirtualBeamer.getUser().loadSessions();
        for (int i = 0; i < VirtualBeamer.getUser().getActiveSession().size(); i++)
            list.add(VirtualBeamer.getUser().getActiveSession().get(i).getSessionName());
        ongoingSessions.getItems().addAll(list);
        sessionNameEditTextField.setFocusTraversable(false);
    }
}
