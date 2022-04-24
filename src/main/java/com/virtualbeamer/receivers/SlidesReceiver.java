package com.virtualbeamer.receivers;

import com.virtualbeamer.utils.Helpers;

import java.io.IOException;
import java.net.*;

import static com.virtualbeamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtualbeamer.constants.SessionConstants.SLIDES_MULTICAST_BASE_PORT;
import static com.virtualbeamer.utils.SlidesHandler.receiveSlides;

public class SlidesReceiver extends Thread {
    final private MulticastSocket socket;
    final private InetSocketAddress inetSocketAddress;
    final private NetworkInterface networkInterface;

    public SlidesReceiver(int port) throws IOException {
        int receiverPort = SLIDES_MULTICAST_BASE_PORT + port;
        socket = new MulticastSocket(receiverPort);
        socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
        inetSocketAddress = new InetSocketAddress(GROUP_ADDRESS, receiverPort);
        networkInterface = Helpers.getNetworkInterface();
    }

    public void run() {
        try {
            socket.joinGroup(inetSocketAddress, networkInterface);
            receiveSlides(socket);
        } catch (IOException e) {
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