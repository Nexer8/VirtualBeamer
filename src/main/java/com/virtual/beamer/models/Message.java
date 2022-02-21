package com.virtual.beamer.models;

import com.virtual.beamer.constants.MessageType;

import java.io.File;
import java.io.Serializable;

public class Message implements Serializable {
    final public MessageType type;
    public File[] slides;
    public Session session;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, File[] slides) {
        this.type = type;
        this.slides = slides;
    }

    public Message(MessageType type, Session session) {
        this.type = type;
        this.session = session;
    }
}
