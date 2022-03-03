package com.virtualbeamer.receivers;

import com.virtualbeamer.constants.SessionConstants;

import java.io.IOException;
import java.net.*;

import static com.virtualbeamer.utils.MessageHandler.collectAndProcessMessage;
import static com.virtualbeamer.utils.PacketCreator.SLIDE_PACKET_MAX_SIZE;

public class UnicastReceiver extends Thread {
    final private DatagramSocket socket;

    public UnicastReceiver() throws IOException {
        socket = new DatagramSocket(SessionConstants.INDIVIDUAL_MESSAGE_PORT);
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
