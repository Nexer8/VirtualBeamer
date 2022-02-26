package com.virtual.beamer.models;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

    public void sendMessage(Message message, InetAddress address) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length, address, INDIVIDUAL_MESSAGE_PORT);
        socket.send(packet);
        System.out.println("Responded to Hello message!");
    }
}
