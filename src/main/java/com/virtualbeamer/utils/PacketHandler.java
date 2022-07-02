package com.virtualbeamer.utils;

import com.virtualbeamer.constants.MessageType;
import com.virtualbeamer.models.Message;
import com.virtualbeamer.models.SlidesReceiverData;
import com.virtualbeamer.services.MainService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static com.virtualbeamer.constants.SessionConstants.*;
import static com.virtualbeamer.utils.MessageHandler.handleMessage;
import static com.virtualbeamer.utils.PacketCreator.MAX_PACKET_SIZE;
import static com.virtualbeamer.utils.SlidesHandler.processReceivedSlideData;

public class PacketHandler extends Thread {

    private final ArrayList<Message> messagesQueue;
    private final ArrayList<byte[]> slidesQueue;

    private final ArrayList<Message> processedMessages;
    private final ArrayList<byte[]> processedSlides;
    private Timer queueFlushTimer;
    private SlidesReceiverData srd;
    private final ArrayList<MessageType> bannedMessageType;

    public PacketHandler() {
        messagesQueue = new ArrayList<>();
        slidesQueue = new ArrayList<>();
        processedMessages = new ArrayList<>();
        processedSlides = new ArrayList<>();
        bannedMessageType = new ArrayList<>();
        bannedMessageType.add(MessageType.IM_ALIVE);
    }

    public void setSlidesReceiverData(SlidesReceiverData srd) {
        this.srd = srd;
    }

    public void handleSlide(byte[] data) {
        if (!this.slidesQueue.contains(data)) {
            this.slidesQueue.add(data);
        }
    }

    public void handlePacket(Message message) {
        if (!bannedMessageType.contains(message.type))
            if (!this.messagesQueue.contains(message))
                this.messagesQueue.add(message);
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

    public void addProcessedSlide(byte[] data) {
        processedSlides.add(data);
    }

    public void resendMessage(InetAddress address, int packetID) throws IOException {
        for (Message m : processedMessages)
            if (m.packetID == packetID) {
                MainService.getInstance().sendMissingMessage(address, m);
                break;
            }
    }

    public void resendSlide(InetAddress address, short session, short slice) throws IOException {
        for (byte[] data : processedSlides)
            if (getSlideSession(data) == session && getSlideSlice(data) == slice) {
                MainService.getInstance().sendMissingSlide(address, data);
                break;
            }
    }

    private short getSlideSession(byte[] data) {
        return (short) (data[1] & 0xff);

    }

    private short getSlideSlice(byte[] data) {
        return (short) (data[5] & 0xff);
    }

    public void run() {
        queueFlushTimer = new Timer(false);
        queueFlushTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                int size;
                ArrayList<byte[]> tempSlidesQueue = new ArrayList<>();
                ArrayList<Message> tempMessageQueue = new ArrayList<>();

                // Slide packet queue handling
                if (!slidesQueue.isEmpty()) {
                    // Copy max 15 element in the temporary queue
                    for (int i = 0; i < Math.min(15, slidesQueue.size()); i++)
                        tempSlidesQueue.add(slidesQueue.get(i));

                    // Find missing slides and retrieve them from leader
                    size = tempSlidesQueue.size();
                    tempSlidesQueue.sort(new PacketSliceComparator());
                    for (int i = 0; i < Math.min(15, size) - 1; i++) {
                        if (getSlideSession(tempSlidesQueue.get(i)) == getSlideSession(tempSlidesQueue.get(i + 1))
                                && getSlideSlice(tempSlidesQueue.get(i + 1)) - getSlideSlice(tempSlidesQueue.get(i)) > 1) {
                            for (int j = 1; j < getSlideSlice(tempSlidesQueue.get(i + 1)) - getSlideSlice(tempSlidesQueue.get(i)); j++) {
                                try {
                                    byte[] packet = new byte[MAX_PACKET_SIZE + 8];
                                    MainService.getInstance().sendPacketLostMessage(
                                            InetAddress.getByName(MainService.getInstance().getGroupSession().getLeaderIPAddress()),
                                            new Message(MessageType.SLIDE_RESEND, getSlideSession(tempSlidesQueue.get(i)),
                                                    (short) (getSlideSlice(tempSlidesQueue.get(i)) + j)));
                                    ServerSocket serverSocket = new ServerSocket(PACKET_LOSS_PORT);
                                    Socket socket = serverSocket.accept();
                                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                                    in.readFully(packet, 0, MAX_PACKET_SIZE + 8);
                                    tempSlidesQueue.add(packet);
                                    socket.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                    tempSlidesQueue.sort(new PacketSliceComparator());

                    // Handle the slides buffer with no missing packets
                    for (int i = 0; i < Math.min(15, tempSlidesQueue.size()); i++) {
                        try {
                            System.out.println("Handling slide ID " + getSlideSession(tempSlidesQueue.get(i))
                                    + " " + getSlideSlice(tempSlidesQueue.get(i)));
                            processReceivedSlideData(tempSlidesQueue.get(i), srd);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // Delete the slides from the processing queue and add them to the processed queue
                    for (byte[] data : tempSlidesQueue) {
                        slidesQueue.remove(data);
                        processedSlides.add(data);
                    }
                }

                // Message queue handling
                if (!messagesQueue.isEmpty()) {
                    System.out.println("-- HANDLING MESSAGE --");
                    // Copy max 15 element in the temporary queue
                    for (int i = 0; i < Math.min(15, messagesQueue.size()); i++)
                        tempMessageQueue.add(messagesQueue.get(i));

                    // Find missing messages and retrieve them from leader
                    size = tempMessageQueue.size();
                    tempMessageQueue.sort(new MessageComparator());
                    for (int i = 0; i < Math.min(15, size) - 1; i++) {
                        if (tempMessageQueue.get(i + 1).packetID - tempMessageQueue.get(i).packetID > 1) {
                            for (int j = 1; j < tempMessageQueue.get(i + 1).packetID - tempMessageQueue.get(i).packetID; j++) {
                                try {
                                    System.out.println("Packet miss found:" + j + tempMessageQueue.get(i).packetID);

                                    MainService.getInstance().sendPacketLostMessage(InetAddress.getByName(
                                                    MainService.getInstance().getGroupSession().getLeaderIPAddress()),
                                            new Message(MessageType.MESSAGE_RESEND, tempMessageQueue.get(i).packetID + j));
                                    ServerSocket serverSocket = new ServerSocket(PACKET_LOSS_PORT);
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

                    // Handle the messages with new packets
                    for (int i = 0; i < Math.min(15, tempMessageQueue.size()); i++) {
                        try {
                            System.out.println("Handling packet ID " + tempMessageQueue.get(i).packetID);
                            handleMessage(tempMessageQueue.get(i), InetAddress.getByName(
                                    MainService.getInstance().getGroupSession().getLeaderIPAddress()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // Delete the messages from the processing queue and add them to the processed queue
                    for (Message m : tempMessageQueue) {
                        messagesQueue.remove(m);
                        processedMessages.add(m);
                    }

                }

                // Delete messages from the processed queues
                if (processedMessages.size() > 100)
                    processedMessages.subList(0, 20).clear();

                if (processedSlides.size() > 100)
                    processedSlides.subList(0, 20).clear();
            }
        }, 0, MESSAGE_QUEUE_FLUSH);
    }
}

class PacketSliceComparator implements Comparator<byte[]> {

    // override the compare() method
    public int compare(byte[] data1, byte[] data2) {
        short session1 = (short) (data1[1] & 0xff);
        short session2 = (short) (data2[1] & 0xff);

        short slice1 = (short) (data1[5] & 0xff);
        short slice2 = (short) (data2[5] & 0xff);

        if (session1 == session2) {
            return slice1 - slice2;
        }

        return session1 - session2;

    }
}

class MessageComparator implements Comparator<Message> {

    @Override
    public int compare(Message m1, Message m2) {
        return m1.packetID - m2.packetID;
    }
}
