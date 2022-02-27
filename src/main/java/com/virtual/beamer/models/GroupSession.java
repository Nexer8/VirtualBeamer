package com.virtual.beamer.models;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static com.virtual.beamer.constants.SessionConstants.GROUP_ADDRESS;

public class GroupSession implements Serializable {
    private String name;
    private int port;
    private String leaderName;
    private String leaderIPAddress;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public GroupSession(String name) throws SocketException {
        this.name = name;
        this.port = 0;
    }

    public String getLeaderInfo() {
        return leaderName + "(" + leaderIPAddress + ")";
    }

    public void setLeaderData(String leaderName, InetAddress leaderAddress) {
        this.leaderName = leaderName;
        this.leaderIPAddress = leaderAddress.getHostAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupSession that = (GroupSession) o;
        return name.equals(that.name);
    }

    public void sendGroupMessage(Message message) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(GROUP_ADDRESS), User.getInstance().getGroupSession().getPort());
        socket.send(packet);
    }
}
