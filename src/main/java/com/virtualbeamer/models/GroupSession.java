package com.virtualbeamer.models;

import com.virtualbeamer.services.MainService;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import static com.virtualbeamer.constants.MessageType.NEXT_SLIDE;
import static com.virtualbeamer.constants.MessageType.PREVIOUS_SLIDE;
import static com.virtualbeamer.constants.SessionConstants.GROUP_ADDRESS;

public class GroupSession implements Serializable {
    private String name;
    private int port;
    private String leaderName;
    private String leaderIPAddress;
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

    public GroupSession(String name) {
        this.name = name;
        this.port = 0;
        this.buffer = new ArrayList<>();
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

    public synchronized void sendGroupMessage(Message message) throws IOException {
        if (message.type == NEXT_SLIDE || message.type == PREVIOUS_SLIDE) {
            if (buffer.isEmpty())
                message.packetID = 1;
            else {
                int maxPacketID = 0;
                Message maxMessage = null;
                for (Message m : buffer) {
                    if (m.packetID > maxPacketID) {
                        maxPacketID = m.packetID;
                        maxMessage = m;
                    }
                }
                message.packetID = maxPacketID + 1;
            }
            buffer.add(message);
        }
        DatagramSocket socket = new DatagramSocket();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        System.out.println("Sending group message to port: "
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
}
