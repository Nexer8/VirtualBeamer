package com.virtual.beamer.models;

import com.virtual.beamer.constants.AppConstants;
import com.virtual.beamer.utils.MessageType;
import com.virtual.beamer.utils.MulticastReceiver;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.Arrays;

public class User {
    private static volatile User instance;
    private File[] slides;
    private int currentSlide = 0;
    private AppConstants.UserType userType;


    private User() throws IOException {
        MulticastReceiver mr = new MulticastReceiver();
        mr.start();
    }

    public static User getInstance() throws IOException {
        User tmp = instance;
        if (tmp != null) {
            return tmp;
        }
        synchronized(User.class) {
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
        session.multicast(new Message(MessageType.SEND_SLIDE,slides));
    }

    public void nextSlide()
    {
        currentSlide++;
    }

    public void previousSlide()
    {
        currentSlide--;
    }

    public int getCurrentSlide()
    {
        return currentSlide;
    }

    public File[] getSlides()
    {
        return slides;
    }

    public AppConstants.UserType getUserType() {
        return userType;
    }

    public void setCurrentSlide(int currentSlide)
    {
        this.currentSlide = currentSlide;
    }

    public void setSlides(File[] slides)
    {
        this.slides = slides;
    }
}
