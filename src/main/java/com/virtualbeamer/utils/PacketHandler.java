package com.virtualbeamer.utils;

import com.virtualbeamer.constants.MessageType;
import com.virtualbeamer.models.Message;
import com.virtualbeamer.services.MainService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static com.virtualbeamer.constants.SessionConstants.*;
import static com.virtualbeamer.utils.MessageHandler.handleMessage;

public class PacketHandler extends Thread {

    public static final int MAX_ELEMENTS_TO_COPY = 15;
    private Timer queueFlushTimer;
    private final ServerSocket serverSocket;

    private final ArrayList<Message> messagesQueue;

    private final ArrayList<Message> processedMessages;
    private final ArrayList<MessageType> bannedMessageType;

    private int lastPacketID;

    public PacketHandler() throws IOException {
        messagesQueue = new ArrayList<>();
        processedMessages = new ArrayList<>();
        bannedMessageType = new ArrayList<>();
        lastPacketID = 1;
        serverSocket = new ServerSocket(PACKET_LOSS_PORT);

        //bannedMessageType.add(MessageType.IM_ALIVE);
    }

    public void handlePacket(Message message) throws IOException {
        if (!bannedMessageType.contains(message.type)) {
            if (!this.messagesQueue.contains(message) && !this.processedMessages.contains(message))
                this.messagesQueue.add(message);
        } else
            handleMessage(message, InetAddress.getByName(
                    MainService.getInstance().getGroupSession().getLeaderIPAddress()));
    }

    public int addProcessedMessage(Message message) {
        if (!bannedMessageType.contains(message.type)) {
            int maxPacketID = 1;
            for (Message m : processedMessages)
                if (m.packetID > maxPacketID)
                    maxPacketID = m.packetID;
            message.packetID = maxPacketID + 1;
            processedMessages.add(message);
            return maxPacketID + 1;
        }

        return 0;
    }

    public void resendMessage(InetAddress address, int packetID) throws IOException {
        for (Message m : processedMessages)
            if (m.packetID == packetID) {
                MainService.getInstance().sendMissingMessage(address, m);
                break;
            }
    }

    public void run() {
        while (true) {
            int size;
            ArrayList<Message> tempMessageQueue = new ArrayList<>();

            // Message queue handling
            if (!messagesQueue.isEmpty()) {
                System.out.println("-- HANDLING MESSAGE --");
                // Copy max 15 element in the temporary queue
                Message fakeMessage = new Message(MessageType.FAKE_PACKET);
                fakeMessage.setPacketID(lastPacketID);
                tempMessageQueue.add(fakeMessage);
                for (int i = 0; i < Math.min(MAX_ELEMENTS_TO_COPY, messagesQueue.size()); i++){
                    tempMessageQueue.add(messagesQueue.get(i));
                }

                // Find missing messages and retrieve them from leader
                size = tempMessageQueue.size();
                tempMessageQueue.sort(new MessageComparator());
                for (int i = 0; i < Math.min(MAX_ELEMENTS_TO_COPY, size) - 1; i++) {
                    if (tempMessageQueue.get(i + 1).packetID - tempMessageQueue.get(i).packetID > 1) {
                        for (int j = 1; j < tempMessageQueue.get(i + 1).packetID - tempMessageQueue.get(i).packetID; j++) {
                            try {
                                System.out.println("Packet miss found:" + (j + tempMessageQueue.get(i).packetID));

                                MainService.getInstance().sendPacketLostMessage(InetAddress.getByName(
                                                MainService.getInstance().getGroupSession().getLeaderIPAddress()),
                                        new Message(MessageType.MESSAGE_RESEND, tempMessageQueue.get(i).packetID + j));
                                Socket socket = serverSocket.accept();
                                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                                Message message = (Message) in.readObject();
                                tempMessageQueue.add(message);
                                System.out.println("Packet " + message.packetID + " received!");
                                socket.close();
                            } catch (IOException | ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                tempMessageQueue.sort(new MessageComparator());
                // Handle the messages with new packets
                for (int i = 0; i < Math.min(MAX_ELEMENTS_TO_COPY, tempMessageQueue.size()); i++) {
                    try {

                        if (tempMessageQueue.get(i).type != MessageType.FAKE_PACKET) {
                            System.out.println("Handling packet ID " + tempMessageQueue.get(i).packetID);
                            handleMessage(tempMessageQueue.get(i), InetAddress.getByName(
                                    MainService.getInstance().getGroupSession().getLeaderIPAddress()));
                        }

                        if (i == Math.min(MAX_ELEMENTS_TO_COPY, tempMessageQueue.size()) - 1)
                            lastPacketID = tempMessageQueue.get(i).packetID;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Delete the messages from the processing queue and add them to the processed queue
                for (Message m : tempMessageQueue) {
                    if (m.type != MessageType.FAKE_PACKET) {
                        messagesQueue.remove(m);
                        processedMessages.add(m);
                    }
                }
                System.out.println("-- END HANDLING message --");
            }

            // Delete messages from the processed queues
            if (processedMessages.size() > 100)
                processedMessages.subList(0, 20).clear();

            try {
                Thread.sleep(MESSAGE_QUEUE_FLUSH);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}

class MessageComparator implements Comparator<Message> {

    @Override
    public int compare(Message m1, Message m2) {
        return m1.packetID - m2.packetID;
    }
}
