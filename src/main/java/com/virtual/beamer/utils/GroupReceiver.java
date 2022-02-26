package com.virtual.beamer.utils;

import com.virtual.beamer.models.Message;

import java.io.IOException;
import java.net.*;

import static com.virtual.beamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtual.beamer.models.Message.deserializeMessage;
import static com.virtual.beamer.models.Message.handleMessage;

public class GroupReceiver extends Thread {
    final private MulticastSocket socket;
    final private InetSocketAddress inetSocketAddress;
    final private NetworkInterface networkInterface;

    public GroupReceiver(int port) throws IOException {
        socket = new MulticastSocket(port);
        socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
        inetSocketAddress = new InetSocketAddress(GROUP_ADDRESS, port);
        networkInterface = Helpers.getNetworkInterface();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            byte[] buffer = new byte[10000];
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
