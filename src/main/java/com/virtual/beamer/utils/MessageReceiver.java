package com.virtual.beamer.utils;

import com.virtual.beamer.models.Message;

import java.io.IOException;
import java.net.*;

import static com.virtual.beamer.constants.SessionConstants.INDIVIDUAL_MESSAGE_PORT;
import static com.virtual.beamer.models.Message.deserializeMessage;
import static com.virtual.beamer.models.Message.handleMessage;

public class MessageReceiver extends Thread {
    final private DatagramSocket socket;

    public MessageReceiver() throws IOException {
        socket = new DatagramSocket(INDIVIDUAL_MESSAGE_PORT);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            byte[] buffer = new byte[10000];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                InetAddress senderAddress = packet.getAddress();

                Message message = deserializeMessage(buffer);
                handleMessage(message, senderAddress);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}
