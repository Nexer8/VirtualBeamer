package com.virtual.beamer.models;

import com.virtual.beamer.constants.AppConstants;
import com.virtual.beamer.constants.MessageType;
import com.virtual.beamer.controllers.PresentationViewController;
import com.virtual.beamer.utils.MessageReceiver;
import com.virtual.beamer.utils.MulticastReceiver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.InetAddress;
import java.net.SocketAddress;

import static com.virtual.beamer.constants.AppConstants.UserType.PRESENTER;
import static com.virtual.beamer.constants.AppConstants.UserType.VIEWER;

public class User {
    private static volatile User instance;
    private ObservableList<File> slides;
    private int currentSlide = 0;
    private AppConstants.UserType userType = VIEWER;
    private PresentationViewController pvc;
    private final ObservableList<Session> sessions = FXCollections.observableArrayList();
    private final ObservableList<String> sessionsNames = FXCollections.observableArrayList();
    private final Session session;

    private User() throws IOException {
        MulticastReceiver mr = new MulticastReceiver();
        mr.start();

        MessageReceiver rec = new MessageReceiver();
        rec.start();
        session = new Session("");
    }

    public static User getInstance() throws IOException {
        if (instance != null) {
            return instance;
        }
        synchronized (User.class) {
            if (instance == null) {
                instance = new User();
                instance.sendHelloMessage();
            }
            return instance;
        }
    }

    public void createSession(String sessionName) throws IOException {
        userType = PRESENTER;
        session.setName(sessionName);
        multicastSessionDetails();
    }

    public void sendHelloMessage() throws IOException {
        session.multicast(new Message(MessageType.HELLO));
    }

    public void multicastSlides() throws IOException {
        session.multicast(new Message(MessageType.SEND_SLIDES, slides.toArray(new File[]{})));
    }

    public void multicastSessionDetails() throws IOException {
        session.multicast(new Message(MessageType.SESSION_DETAILS, session));
    }

    public void sendSessionDetails(InetAddress senderAddress) throws IOException {
        session.sendMessage(new Message(MessageType.SESSION_DETAILS, session), senderAddress);
    }

    public void multicastDeleteSession() throws IOException {
        session.multicast(new Message(MessageType.DELETE_SESSION, session));
        userType = VIEWER;
    }

    public void multicastNextSlide() throws IOException {
        currentSlide++;
        session.multicast(new Message(MessageType.NEXT_SLIDE));
    }

    public void multicastPreviousSlide() throws IOException {
        currentSlide--;
        session.multicast(new Message(MessageType.PREVIOUS_SLIDE));
    }

    public int getCurrentSlide() {
        return currentSlide;
    }

    public ObservableList<File> getSlides() {
        return slides;
    }

    public AppConstants.UserType getUserType() {
        return userType;
    }

    public void setCurrentSlide(int currentSlide) throws IOException {
        this.currentSlide = currentSlide;

        if (userType == VIEWER) {
            pvc.setSlide();
        }
    }

    public void setSlides(File[] slides) throws IOException {
        this.slides = FXCollections.observableArrayList(slides);

        if (userType == VIEWER) {
            if (pvc.getProgressIndicator().isVisible()) {
                pvc.getProgressIndicator().setVisible(false);
            }
            pvc.setSlide();
        }
    }

    public void setUserType(AppConstants.UserType userType) {
        this.userType = userType;
    }

    public void setPvc(PresentationViewController pvc) {
        this.pvc = pvc;
    }

    public void addSessionData(Session session) {
        sessions.add(session);

        Platform.runLater(() -> sessionsNames.add(session.getName()));
        System.out.println(session.getName());
    }

    public ObservableList<String> getSessionsNames() {
        return sessionsNames;
    }

    public void deleteSession(Session session) {
        int idx = sessions.indexOf(session);
        if (idx != -1) {
            String name = sessions.get(idx).getName();
            sessions.remove(idx);

            Platform.runLater(() -> sessionsNames.remove(name));
        }
    }
}
