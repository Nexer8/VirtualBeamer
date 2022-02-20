package com.virtual.beamer.utils;

import com.virtual.beamer.models.Message;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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
    private void handleMessage(Message message) {
        switch (message.type) {
            case HELLO -> System.out.println(message.type.name());
            case SEND_SLIDE -> System.out.println(message.type.name());
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
            byte[] buffer = new byte[256];
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
