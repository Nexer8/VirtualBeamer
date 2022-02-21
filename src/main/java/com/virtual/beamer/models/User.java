package com.virtual.beamer.models;

import com.virtual.beamer.constants.AppConstants;
import com.virtual.beamer.constants.MessageType;
import com.virtual.beamer.controllers.PresentationViewController;
import com.virtual.beamer.utils.MulticastReceiver;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;

import static com.virtual.beamer.constants.AppConstants.UserType.VIEWER;

public class User {
    private static volatile User instance;
    private ObservableList<File> slides;
    private int currentSlide = 0;
    private AppConstants.UserType userType;
    private PresentationViewController pvc;

    private User() throws IOException {
        MulticastReceiver mr = new MulticastReceiver();
        mr.start();
    }

    public static User getInstance() throws IOException {
        if (instance != null) {
            return instance;
        }
        synchronized (User.class) {
            if (instance == null) {
                instance = new User();
            }
            return instance;
        }
    }

    public void establishSession() throws IOException {
        Session session = new Session();
        session.multicast(new Message(MessageType.HELLO));
    }

    public void multicastSlides() throws IOException {
        Session session = new Session();
        session.multicast(new Message(MessageType.SEND_SLIDES, slides.toArray(new File[]{})));
    }

    public void multicastNextSlide() throws IOException {
        currentSlide++;
        Session session = new Session();
        session.multicast(new Message(MessageType.NEXT_SLIDE));
    }

    public void multicastPreviousSlide() throws IOException {
        currentSlide--;
        Session session = new Session();
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
}
