package com.virtualbeamer.receivers;

import com.virtualbeamer.models.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;

import static com.virtualbeamer.constants.SessionConstants.INDIVIDUAL_MESSAGE_PORT;
import static com.virtualbeamer.utils.MessageHandler.handleMessage;

public class UnicastReceiver extends Thread {
    final private ServerSocket serverSocket;

    public UnicastReceiver() throws IOException {
        serverSocket = new ServerSocket(INDIVIDUAL_MESSAGE_PORT);
    }

    public void run() {
        try {
            // noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Message message = (Message) in.readObject();
                handleMessage(message, socket.getInetAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
