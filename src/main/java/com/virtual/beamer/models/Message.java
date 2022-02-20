package com.virtual.beamer.models;

import com.virtual.beamer.utils.MessageType;

import java.io.File;
import java.io.Serializable;

public class Message implements Serializable {
    public MessageType type;
    public File[] payload; // TODO: this will be used to send slides

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, File[] payload)
    {
        this.type = type;
        this.payload = payload;
    }
}
