package com.virtual.beamer.models;

import com.virtual.beamer.utils.MessageType;

import java.io.Serializable;

public class Message implements Serializable {
    public MessageType type;
    private Byte[] payload; // TODO: this will be used to send slides

    public Message(MessageType type) {
        this.type = type;
    }
}
