package com.virtual.beamer.models;

import com.virtual.beamer.utils.MessageType;
import com.virtual.beamer.utils.MulticastReceiver;

import java.io.IOException;

public class User {
    private static volatile User instance;

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
}
