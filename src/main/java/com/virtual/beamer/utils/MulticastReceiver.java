package com.virtual.beamer.utils;

import com.virtual.beamer.models.Message;

import java.io.*;
import java.net.*;

import static com.virtual.beamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtual.beamer.constants.SessionConstants.MULTICAST_PORT;
import static com.virtual.beamer.models.Message.deserializeMessage;
import static com.virtual.beamer.models.Message.handleMessage;

public class MulticastReceiver extends Thread {
    public static final int INET_SOCKET_PORT = 1234;

    final private MulticastSocket socket;
    final private InetSocketAddress inetSocketAddress;
    final private NetworkInterface networkInterface;

    public MulticastReceiver() throws IOException {
        socket = new MulticastSocket(MULTICAST_PORT);
//        TODO: uncomment when ready
//        socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
        inetSocketAddress = new InetSocketAddress(GROUP_ADDRESS, INET_SOCKET_PORT);
        networkInterface = NetworkInterface.getByIndex(0);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            byte[] buffer = new byte[1000];
            socket.joinGroup(inetSocketAddress, networkInterface);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                InetAddress senderAddress = packet.getAddress();
                System.out.println("Sender Socket address:" + senderAddress);

                Message message = deserializeMessage(buffer);
                handleMessage(message, senderAddress);
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
