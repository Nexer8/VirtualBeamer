package com.virtual.beamer.utils;

import com.virtual.beamer.models.Message;
import com.virtual.beamer.models.User;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

import static com.virtual.beamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtual.beamer.models.Message.deserializeMessage;
import static com.virtual.beamer.models.Message.handleMessage;

public class GroupReceiver extends Thread {
    final private MulticastSocket socket;
    final private InetSocketAddress inetSocketAddress;
    final private NetworkInterface networkInterface;
    private ArrayList<Message> buffer;

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
            byte[] buffer = new byte[10000];
            socket.joinGroup(inetSocketAddress, networkInterface);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                InetAddress senderAddress = packet.getAddress();
                System.out.println("Groupcast received packet from " + senderAddress + ":" + inetSocketAddress.getPort());

                Message message = deserializeMessage(buffer);
                if (!this.buffer.contains(message)) {
                    this.buffer.add(message);
                    if (this.buffer.size() >= 2) {
                        if (this.buffer.get(this.buffer.size() - 1).packetID - 1 > this.buffer.get(this.buffer.size() - 2).packetID) {
                            System.out.println("Missing packet found");
                            User.getInstance().sendNACKPacket(this.buffer.get(this.buffer.size() - 1).packetID - 1);
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
