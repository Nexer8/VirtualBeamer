package com.virtual.beamer;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Session implements Serializable {

    private String sessionName;
    private String sessionIP;

    public Session(String sessionName) throws UnknownHostException {
        this.sessionName = sessionName;
        this.sessionIP = InetAddress.getLocalHost().getHostAddress();
    }

    public Session(String sessionName, String sessionIP)
    {
        this.sessionName = sessionName;
        this.sessionIP = sessionIP;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getSessionIP() {
        return sessionIP;
    }
}
