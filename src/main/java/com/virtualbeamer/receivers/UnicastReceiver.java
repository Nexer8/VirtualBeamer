package com.virtualbeamer.receivers;

import java.io.IOException;
import java.net.*;

import static com.virtualbeamer.constants.SessionConstants.INDIVIDUAL_MESSAGE_PORT;
import static com.virtualbeamer.utils.MessageHandler.collectAndProcessMessage;
import static com.virtualbeamer.utils.PacketCreator.SLIDE_PACKET_MAX_SIZE;

public class UnicastReceiver extends Thread {
    final private DatagramSocket socket;

    public UnicastReceiver() throws IOException {
        socket = new DatagramSocket(INDIVIDUAL_MESSAGE_PORT);
    }

    public void run() {
        try {
            byte[] buffer = new byte[SLIDE_PACKET_MAX_SIZE + 8];
            collectAndProcessMessage(socket, buffer);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}
