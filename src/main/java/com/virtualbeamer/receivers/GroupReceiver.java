package com.virtualbeamer.receivers;

import com.virtualbeamer.models.Message;
import com.virtualbeamer.services.MainService;
import com.virtualbeamer.utils.Helpers;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

import static com.virtualbeamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtualbeamer.utils.MessageHandler.deserializeMessage;
import static com.virtualbeamer.utils.MessageHandler.handleMessage;
import static com.virtualbeamer.utils.PacketCreator.MAX_PACKET_SIZE;

public class GroupReceiver extends Thread {
    final private MulticastSocket socket;
    final private InetSocketAddress inetSocketAddress;
    final private NetworkInterface networkInterface;
    private final ArrayList<Message> buffer;

    public GroupReceiver(int port) throws IOException {
        socket = new MulticastSocket(port);
        socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
        inetSocketAddress = new InetSocketAddress(GROUP_ADDRESS, port);
        networkInterface = Helpers.getNetworkInterface();
        buffer = new ArrayList<>();
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
                if (!this.buffer.contains(message)) {
                    this.buffer.add(message);
                    if (this.buffer.size() >= 2) {
                        if (this.buffer.get(this.buffer.size() - 1).packetID
                                - 1 > this.buffer.get(this.buffer.size() - 2).packetID) {
                            System.out.println("Missing packet found");
                            MainService.getInstance()
                                    .sendNACKPacket(this.buffer.get(this.buffer.size() - 1).packetID - 1);
                        }
                    }
                    handleMessage(message, senderAddress);
                }
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
