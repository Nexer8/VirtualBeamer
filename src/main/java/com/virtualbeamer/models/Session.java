package com.virtualbeamer.models;

import com.virtualbeamer.constants.SessionConstants;

import java.io.*;
import java.net.*;

public class Session implements Serializable {
    private final DatagramSocket socket;

    public Session() throws SocketException {
        socket = new DatagramSocket();
    }

    public void multicast(Message message) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(SessionConstants.GROUP_ADDRESS), SessionConstants.MULTICAST_PORT);
        socket.send(packet);
    }

    public void sendMessage(Message message, InetAddress address, int port) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        System.out.println("Responded to Hello message!");
    }

    public void sendMessage(Message message, InetAddress address) throws IOException {
        sendMessage(message, address, SessionConstants.INDIVIDUAL_MESSAGE_PORT);
    }
}
