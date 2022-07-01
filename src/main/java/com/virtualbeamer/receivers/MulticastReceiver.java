package com.virtualbeamer.receivers;

import com.virtualbeamer.utils.Helpers;

import java.io.*;
import java.net.*;

import static com.virtualbeamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtualbeamer.constants.SessionConstants.MULTICAST_PORT;
import static com.virtualbeamer.utils.MessageHandler.collectAndProcessMultipleMessages;

public class MulticastReceiver extends Thread {
    final private MulticastSocket socket;
    final private InetSocketAddress inetSocketAddress;
    private final NetworkInterface networkInterface;

    public MulticastReceiver() throws IOException {
        socket = new MulticastSocket(MULTICAST_PORT);
        socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
        inetSocketAddress = new InetSocketAddress(GROUP_ADDRESS, MULTICAST_PORT);
        networkInterface = Helpers.getNetworkInterface();
    }

    public void run() {
        try {
            byte[] buffer = new byte[2000];
            System.out.println(networkInterface);
            socket.joinGroup(inetSocketAddress, networkInterface);
            collectAndProcessMultipleMessages(socket, buffer);
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
