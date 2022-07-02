package com.virtualbeamer.models;

import com.virtualbeamer.constants.MessageType;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;

public class Message implements Serializable {
    final public MessageType type;
    public GroupSession session;
    public int intVariable;
    public String stringVariable;
    public InetAddress ipAddress;
    public int packetID;
    public short shortVariable1;
    public short shortVariable2;
    public ArrayList<Integer> groupIDs;
    public ArrayList<String> usernames;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, GroupSession session) {
        this.type = type;
        this.session = session;
    }

    public Message(MessageType type, int intVariable) {
        this.type = type;
        this.intVariable = intVariable;
    }

    public Message(MessageType type, short shortVariable1, short shortVariable2) {
        this.type = type;
        this.shortVariable1 = shortVariable1;
        this.shortVariable2 = shortVariable2;
    }

    public Message(MessageType type, String stringVariable) {
        this.type = type;
        this.stringVariable = stringVariable;
    }

    public Message(MessageType type, InetAddress ipAddress) {
        this.type = type;
        this.ipAddress = ipAddress;
    }

    public Message(MessageType type, GroupSession session, String stringVariable) {
        this.type = type;
        this.session = session;
        this.stringVariable = stringVariable;
    }

    public Message(MessageType type, String stringVariable, InetAddress ipAddress) {
        this.type = type;
        this.stringVariable = stringVariable;
        this.ipAddress = ipAddress;
    }

    public Message(MessageType type, int intVariable, InetAddress ipAddress) {
        this.type = type;
        this.intVariable = intVariable;
        this.ipAddress = ipAddress;
    }

    public Message(MessageType type, String stringVariable, int intVariable, InetAddress ipAddress) {
        this.type = type;
        this.stringVariable = stringVariable;
        this.intVariable = intVariable;
        this.ipAddress = ipAddress;
    }

    public Message(MessageType type, GroupSession session, int intVariable, String stringVariable, InetAddress ipAddress) {
        this.type = type;
        this.session = session;
        this.intVariable = intVariable;
        this.stringVariable = stringVariable;
        this.ipAddress = ipAddress;
    }

    public Message(MessageType userIds, ArrayList<String> usernames, ArrayList<Integer> groupIDs, InetAddress inetAddress) {
        this.type = userIds;
        this.usernames = usernames;
        this.groupIDs = groupIDs;
        this.ipAddress = inetAddress;
    }
}
