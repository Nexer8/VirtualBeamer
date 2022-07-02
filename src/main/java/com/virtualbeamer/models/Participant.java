package com.virtualbeamer.models;

import java.net.InetAddress;

public class Participant {
    public int ID;
    public InetAddress ipAddress;

    public Participant(int ID, InetAddress ipAddress) {
        this.ID = ID;
        this.ipAddress = ipAddress;
    }
}
