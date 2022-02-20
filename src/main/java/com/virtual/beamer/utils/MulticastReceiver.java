package com.virtual.beamer.utils;

import com.virtual.beamer.controllers.InitialViewController;
import com.virtual.beamer.controllers.PresentationViewController;
import com.virtual.beamer.models.Message;
import com.virtual.beamer.models.User;

import java.io.*;
import java.net.*;

import static com.virtual.beamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtual.beamer.constants.SessionConstants.MULTICAST_PORT;

public class MulticastReceiver extends Thread {
    public static final int INET_SOCKET_PORT = 1234;

    final private MulticastSocket socket;
    final private InetSocketAddress inetSocketAddress;
    final private NetworkInterface networkInterface;

    public MulticastReceiver() throws IOException {
        socket = new MulticastSocket(MULTICAST_PORT);
        inetSocketAddress = new InetSocketAddress(GROUP_ADDRESS, INET_SOCKET_PORT);
        networkInterface = NetworkInterface.getByIndex(0);
    }

    // Just an example handling of the incoming message
    private void handleMessage(Message message) throws IOException {
        switch (message.type) {
            case HELLO -> System.out.println(message.type.name());
            case SEND_SLIDE -> {
                User.getInstance().setSlides(message.payload);
            }
        }
    }

    private Message deserializeMessage(byte[] buffer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));

        return (Message) is.readObject();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            byte[] buffer = new byte[100000000];
            socket.joinGroup(inetSocketAddress, networkInterface);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Message message = deserializeMessage(buffer);
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.leaveGroup(inetSocketAddress, networkInterface);
            } catch (IOException e) {
                e.printStackTrace();
            }
            socket.close();
        }
    }
}
