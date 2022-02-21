package com.virtual.beamer.models;

import com.virtual.beamer.constants.MessageType;

import java.io.File;
import java.io.Serializable;

public class Message implements Serializable {
    final public MessageType type;
    public File[] payload;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, File[] payload)
    {
        this.type = type;
        this.payload = payload;
    }
}
