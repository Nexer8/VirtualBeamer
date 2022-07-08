package com.virtualbeamer.utils;

import com.virtualbeamer.constants.MessageType;
import com.virtualbeamer.models.Message;
import com.virtualbeamer.models.SlidesReceiverData;
import com.virtualbeamer.services.MainService;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static com.virtualbeamer.constants.SessionConstants.*;
import static com.virtualbeamer.utils.PacketCreator.MAX_PACKET_SIZE;
import static com.virtualbeamer.utils.SlidesHandler.processReceivedSlideData;

public class SlidesPacketLossHandler extends Thread {

    public static final int MAX_ELEMENTS_TO_COPY = 15;
    private final ServerSocket serverSocket;
    private final ArrayList<byte[]> slidesQueue;
    private final ArrayList<byte[]> processedSlides;
    private Timer queueFlushTimer;
    private SlidesReceiverData srd;
    private ServerSocket serverSocket;


    public SlidesPacketLossHandler() throws IOException {
        slidesQueue = new ArrayList<>();
        processedSlides = new ArrayList<>();
        serverSocket = new ServerSocket(SLIDE_LOSS_PORT);

    }

    public void setSlidesReceiverData(SlidesReceiverData srd) {
        this.srd = srd;
    }

    public void handleSlide(byte[] data) {
        if (!this.slidesQueue.contains(data) && !this.processedSlides.contains(data)) {
            byte[] tmp = new byte[MAX_PACKET_SIZE + 8];
            System.arraycopy(data, 0, tmp, 0, data.length);
            this.slidesQueue.add(tmp);
        }
    }

    public void addProcessedSlide(byte[] data) {
        byte[] tmp = new byte[MAX_PACKET_SIZE + 8];
        System.arraycopy(data, 0, tmp, 0, data.length);
        processedSlides.add(tmp);
    }


    public void resendSlide(InetAddress address, short session, short slice) throws IOException {
        for (byte[] data : processedSlides)
            if (getSlideSession(data) == session && getSlideSlice(data) == slice) {
                MainService.getInstance().sendMissingSlide(address, data);
                break;
            }
    }

    public static short getSlideSession(byte[] data) {
        return (short) (data[1] & 0xff);

    }

    public static short getSlideSlice(byte[] data) {
        return (short) (data[5] & 0xff);
    }

    public void run() {
        while(true){

            int size;
            ArrayList<byte[]> tempSlidesQueue = new ArrayList<>();

            // Slide packet queue handling
            if (!slidesQueue.isEmpty()) {
                System.out.println("-- HANDLING SLIDES --");

                // Copy max 15 element in the temporary queue
                for (int i = 0; i < Math.min(MAX_ELEMENTS_TO_COPY, slidesQueue.size()); i++)
                    tempSlidesQueue.add(slidesQueue.get(i));

                // Find missing slides and retrieve them from leader
                size = tempSlidesQueue.size();
                tempSlidesQueue.sort(new PacketSliceComparator());
                for (int i = 0; i < Math.min(MAX_ELEMENTS_TO_COPY, size) - 1; i++) {
                    if (getSlideSession(tempSlidesQueue.get(i)) == getSlideSession(tempSlidesQueue.get(i + 1))
                            && getSlideSlice(tempSlidesQueue.get(i + 1)) - getSlideSlice(tempSlidesQueue.get(i)) > 1) {
                        for (int j = 1; j < getSlideSlice(tempSlidesQueue.get(i + 1)) - getSlideSlice(tempSlidesQueue.get(i)); j++) {
                            try {
                                System.out.println("Slide miss found:" + getSlideSession(tempSlidesQueue.get(i)) + " " + (getSlideSlice(tempSlidesQueue.get(i)) + j));

                                MainService.getInstance().sendPacketLostMessage(
                                        InetAddress.getByName(MainService.getInstance().getGroupSession().getLeaderIPAddress()),
                                        new Message(MessageType.SLIDE_RESEND, getSlideSession(tempSlidesQueue.get(i)),
                                                (short) (getSlideSlice(tempSlidesQueue.get(i)) + j)));
                                Socket socket = serverSocket.accept();
                                DataInputStream in = new DataInputStream(socket.getInputStream());
                                int length = in.readInt();
                                byte[] packet = new byte[length];
                                in.readFully(packet, 0, length);
                                tempSlidesQueue.add(packet);
                                socket.close();
                                System.out.println("Slide miss received");

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
                tempSlidesQueue.sort(new PacketSliceComparator());

                // Handle the slides buffer with no missing packets
                for (int i = 0; i < Math.min(MAX_ELEMENTS_TO_COPY, tempSlidesQueue.size()); i++) {
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
                    if(((short) (data[0] & 0xff)) != 2) {
                        slidesQueue.remove(data);
                        byte[] tmp = new byte[MAX_PACKET_SIZE + 8];
                        System.arraycopy(data, 0, tmp, 0, data.length);
                        processedSlides.add(tmp);
                    }
                }
                System.out.println("-- END HANDLING SLIDES --");

            }

            if (processedSlides.size() > 100)
                processedSlides.subList(0, 20).clear();

            try {
                Thread.sleep(MESSAGE_QUEUE_FLUSH);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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