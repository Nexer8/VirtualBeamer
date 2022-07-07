package com.virtualbeamer.models;

import com.virtualbeamer.services.MainService;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static com.virtualbeamer.constants.SessionConstants.GROUP_ADDRESS;

public class GroupSession implements Serializable {
    private String name;
    private int port;
    private String leaderName;
    private String leaderIPAddress;
    private int leaderID;
    private String previousLeaderIPAddress;

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

     public void updatePreviousLeaderIpAddress(String leaderIPAddress) {
        previousLeaderIPAddress = leaderIPAddress;
    }

    public String getPreviousLeaderIpAddress() {
        return previousLeaderIPAddress;
    }

    public GroupSession(String name) {
        this.name = name;
        this.port = 0;
    }

    public String getLeaderInfo() {
        return leaderName + "(" + leaderIPAddress + ")";
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderData(Participant leader) {
        this.leaderName = leader.name;
        this.leaderIPAddress = leader.ipAddress.getHostAddress();
        this.leaderID = leader.ID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GroupSession that = (GroupSession) o;
        return name.equals(that.name) && port == that.port;
    }

    public synchronized void sendGroupMessage(Message message) throws IOException {

        message.packetID = MainService.getInstance().getPacketHandler().addProcessedMessage(message);

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

    public int getLeaderID() {
        return leaderID;
    }
}
