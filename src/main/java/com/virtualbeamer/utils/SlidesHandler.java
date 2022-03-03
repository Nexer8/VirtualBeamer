package com.virtualbeamer.utils;

import com.virtualbeamer.services.MainService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Objects;

import static com.virtualbeamer.utils.PacketCreator.*;

public class SlidesHandler {
    public static void receiveSlides(DatagramSocket socket) throws IOException {
        int currentSession = -1;
        int slicesStored = 0;
        int[] slicesCol = null;
        byte[] imageData = null;
        boolean sessionAvailable = false;
        byte[] buffer = new byte[SLIDE_PACKET_MAX_SIZE + 8];

        //noinspection InfiniteLoopStatement
        while (true) {
            /* Receive a UDP packet */
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            byte[] data = packet.getData();

            /* Read header information */
            short session = (short) (data[1] & 0xff);
            short slices = (short) (data[2] & 0xff);
            int maxPacketSize = (data[3] & 0xff) << 8 | (data[4] & 0xff); // mask
            // the
            // sign
            // bit
            short slice = (short) (data[5] & 0xff);
            int size = (data[6] & 0xff) << 8 | (data[7] & 0xff); // mask

            /* If SESSION_START flag is set, setup start values */
            if ((data[0] & SESSION_START) == SESSION_START) {
                if (session != currentSession) {
                    currentSession = session;
                    slicesStored = 0;
                    /* Construct an appropriately sized byte array */
                    imageData = new byte[slices * maxPacketSize];
                    slicesCol = new int[slices];
                    sessionAvailable = true;
                }
            }

            /* If package belongs to current session */
            if (sessionAvailable && session == currentSession) {
                if (slicesCol != null && slicesCol[slice] == 0) {
                    slicesCol[slice] = 1;
                    System.arraycopy(data, HEADER_SIZE, imageData, slice
                            * maxPacketSize, size);
                    slicesStored++;
                }
            }
            constructImage(currentSession, slicesStored, imageData, slices);
        }
    }

    static void constructImage(int currentSession, int slicesStored, byte[] imageData, short slices) throws IOException {
        if (slicesStored == slices) {
            ByteArrayInputStream bis = new ByteArrayInputStream(Objects.requireNonNull(imageData));
            BufferedImage image = ImageIO.read(bis);

            synchronized (MainService.class) {
                MainService.getInstance().getSlides().add(image);
            }
            System.out.println("Image " + currentSession + " downloaded");
        }
    }
}
