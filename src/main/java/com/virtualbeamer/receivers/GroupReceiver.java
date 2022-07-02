package com.virtualbeamer.receivers;

import com.virtualbeamer.models.Message;
import com.virtualbeamer.services.MainService;
import com.virtualbeamer.utils.Helpers;

import java.io.IOException;
import java.net.*;

import static com.virtualbeamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtualbeamer.utils.MessageHandler.deserializeMessage;
import static com.virtualbeamer.utils.PacketCreator.MAX_PACKET_SIZE;

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
            byte[] buffer = new byte[MAX_PACKET_SIZE + 8];
            socket.joinGroup(inetSocketAddress, networkInterface);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                InetAddress senderAddress = packet.getAddress();
                System.out
                        .println("Groupcast received packet from " + senderAddress + ":" + inetSocketAddress.getPort());

                Message message = deserializeMessage(buffer);
                MainService.getInstance().handleMessage(message);
                //handleMessage(message, senderAddress);
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
