package com.virtual.beamer.models;

import java.io.*;
import java.net.*;

import static com.virtual.beamer.constants.SessionConstants.*;

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
                InetAddress.getByName(GROUP_ADDRESS), MULTICAST_PORT);
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
        sendMessage(message, address, INDIVIDUAL_MESSAGE_PORT);
    }
}
