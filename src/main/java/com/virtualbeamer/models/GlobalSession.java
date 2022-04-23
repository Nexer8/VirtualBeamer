package com.virtualbeamer.models;

import java.io.*;
import java.net.*;

import static com.virtualbeamer.constants.SessionConstants.*;

public class GlobalSession implements Serializable {
    private final DatagramSocket socket;

    public GlobalSession() throws SocketException {
        socket = new DatagramSocket();
    }

    public synchronized void multicast(Message message) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(GROUP_ADDRESS), MULTICAST_PORT);
        socket.send(packet);
    }

    public synchronized void sendMessage(Message message, InetAddress address, int port) throws IOException {
        try (Socket socket = new Socket(address, port)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(message);
        }
        System.out.println("Responded to Hello message!");
    }

    public void sendMessage(Message message, InetAddress address) throws IOException {
        sendMessage(message, address, INDIVIDUAL_MESSAGE_PORT);
    }
}
