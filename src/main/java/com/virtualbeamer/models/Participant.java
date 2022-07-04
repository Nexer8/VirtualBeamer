package com.virtualbeamer.models;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

public class Participant implements Serializable {
    public String name;
    public int ID;
    public InetAddress ipAddress;

    public Participant(String name, int ID, InetAddress ipAddress) {
        this.name = name;
        this.ID = ID;
        this.ipAddress = ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return ID == that.ID && name.equals(that.name) && ipAddress.equals(that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ID, ipAddress);
    }

    @Override
    public String toString() {
        return name;
    }
}
