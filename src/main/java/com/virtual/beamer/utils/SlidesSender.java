package com.virtual.beamer.utils;

import java.io.*;
import java.net.*;

import static com.virtual.beamer.constants.SessionConstants.*;

public class SlidesSender implements Serializable {
    private final DatagramSocket socket;

    public SlidesSender() throws SocketException {
        socket = new DatagramSocket();
    }

    public void multicast(byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(GROUP_ADDRESS), SLIDES_MULTICAST_PORT);
        socket.send(packet);
    }

    public void sendMessage(byte[] data, InetAddress address) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, INDIVIDUAL_SLIDES_PORT);
        socket.send(packet);
    }
}
