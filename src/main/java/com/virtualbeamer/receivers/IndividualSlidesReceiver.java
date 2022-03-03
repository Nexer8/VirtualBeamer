package com.virtualbeamer.receivers;

import java.io.IOException;
import java.net.DatagramSocket;

import static com.virtualbeamer.constants.SessionConstants.INDIVIDUAL_SLIDES_PORT;
import static com.virtualbeamer.utils.SlidesHandler.receiveSlides;

public class IndividualSlidesReceiver extends Thread {
    final private DatagramSocket socket;

    public IndividualSlidesReceiver() throws IOException {
        socket = new DatagramSocket(INDIVIDUAL_SLIDES_PORT);
    }

    public void run() {
        try {
            receiveSlides(socket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}