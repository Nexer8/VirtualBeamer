package com.virtualbeamer.receivers;

import com.virtualbeamer.models.SlidesReceiverData;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static com.virtualbeamer.constants.SessionConstants.INDIVIDUAL_SLIDES_PORT;
import static com.virtualbeamer.utils.SlidesHandler.processReceivedSlideData;

public class IndividualSlidesReceiver extends Thread {
    public void run() {
        SlidesReceiverData srd = new SlidesReceiverData();

        try (ServerSocket serverSocket = new ServerSocket(INDIVIDUAL_SLIDES_PORT)) {
            // noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream in = new DataInputStream(socket.getInputStream());

                int numberOfSlides = in.readInt();
                for (int i = 0; i < numberOfSlides; i++) {
                    int numberOfPackets = in.readInt();
                    for (int j = 0; j < numberOfPackets; j++) {
                        int length = in.readInt();
                        byte[] data = new byte[length];
                        in.readFully(data, 0, data.length); // read the message

                        processReceivedSlideData(data, srd);
                    }
                }
                System.out.println("Slides received!");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
