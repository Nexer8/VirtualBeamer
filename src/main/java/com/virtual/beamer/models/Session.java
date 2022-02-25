package com.virtual.beamer.models;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.*;

import static com.virtual.beamer.constants.SessionConstants.*;

public class Session implements Serializable {
    public void multicast(Message message) throws IOException {
        DatagramSocket socket = new DatagramSocket();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(GROUP_ADDRESS), MULTICAST_PORT);
        socket.send(packet);
        socket.close();
    }

    public void sendMessage(Message message, InetAddress address) throws IOException {
        DatagramSocket socket = new DatagramSocket();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length, address, INDIVIDUAL_MESSAGE_PORT);
        socket.send(packet);
        System.out.println("Responded to Hello message!");

        socket.close();
    }
}
