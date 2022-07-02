package com.virtualbeamer.models;

import com.virtualbeamer.services.MainService;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import static com.virtualbeamer.constants.SessionConstants.GROUP_ADDRESS;

public class GroupSession implements Serializable {
    private String name;
    private int port;
    private String leaderName;
    private String leaderIPAddress;
    private int leaderID;
    private String[] previousLeaderIPAddress;
    private final ArrayList<Message> buffer;

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

    public String getLeaderIPAddress() {
        return leaderIPAddress;
    }

    public void updatePreviousLeaderIpAddress() {
        previousLeaderIPAddress[0] = previousLeaderIPAddress[1];
        previousLeaderIPAddress[1] = leaderIPAddress;
    }

    public void updatePreviousLeaderIpAddress(String leaderIPAddress) {
        previousLeaderIPAddress[0] = previousLeaderIPAddress[1];
        previousLeaderIPAddress[1] = leaderIPAddress;
    }

    public String getPreviousLeaderIpAddress() {
        return previousLeaderIPAddress[0];
    }

    public GroupSession(String name) {
        this.name = name;
        this.port = 0;
        this.buffer = new ArrayList<>();
        this.previousLeaderIPAddress = new String[2];
    }

    public String getLeaderInfo() {
        return leaderName + "(" + leaderIPAddress + ")";
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderData(String leaderName, InetAddress leaderAddress, int leaderID) {
        this.leaderName = leaderName;
        this.leaderIPAddress = leaderAddress.getHostAddress();
        this.leaderID = leaderID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GroupSession that = (GroupSession) o;
        return name.equals(that.name);
    }

    public synchronized void sendGroupMessage(Message message) throws IOException {
        if (buffer.isEmpty())
            message.packetID = 1;
        else
            message.packetID = buffer.get(buffer.size() - 1).packetID + 1;

        buffer.add(message);
        DatagramSocket socket = new DatagramSocket();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        System.out.println("[Packet " + message.packetID + "]Sending group message to port: "
                + MainService.getInstance().getGroupSession().getPort());
        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(GROUP_ADDRESS), MainService.getInstance().getGroupSession().getPort());
        socket.send(packet);
        socket.close();
    }

    public void sendGroupMessage(int packetID) throws IOException {
        for (Message m : this.buffer) {
            if (m.packetID == packetID) {
                sendGroupMessage(m);
                break;
            }
        }
    }

    public int getLeaderID() {
        return leaderID;
    }
}
