package com.virtualbeamer.utils;

import com.virtualbeamer.models.SlidesReceiverData;
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
    public synchronized static void receiveSlides(DatagramSocket socket) throws IOException {
        SlidesReceiverData srd = new SlidesReceiverData();

        // noinspection InfiniteLoopStatement
        while (true) {
            /* Receive a UDP packet */
            DatagramPacket packet = new DatagramPacket(srd.buffer, srd.buffer.length);
            socket.receive(packet);
            byte[] data = packet.getData();
            MainService.getInstance().getSlidesPacketLossHandler().setSlidesReceiverData(srd);
            MainService.getInstance().getSlidesPacketLossHandler().handleSlide(data);
            //processReceivedSlideData(data, srd);
        }
    }

    public static void processReceivedSlideData(byte[] data, SlidesReceiverData srd) throws IOException {
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
            if (session != srd.currentSession) {
                srd.currentSession = session;
                srd.slicesStored = 0;
                /* Construct an appropriately sized byte array */
                srd.imageData = new byte[slices * maxPacketSize];
                srd.slicesCol = new int[slices];
                srd.sessionAvailable = true;
            }
        }

        /* If package belongs to current session */
        if (srd.sessionAvailable && session == srd.currentSession) {
            if (srd.slicesCol != null && srd.slicesCol[slice] == 0) {
                srd.slicesCol[slice] = 1;
                System.arraycopy(data, HEADER_SIZE, srd.imageData, slice
                        * maxPacketSize, size);
                srd.slicesStored++;
            }
        }
        if (srd.slicesStored == slices) {
            System.out.println("Image " + srd.currentSession + " downloaded");
            constructImage(srd.imageData);
        }
    }

    public static void constructImage(byte[] imageData) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(Objects.requireNonNull(imageData));
        BufferedImage image = ImageIO.read(bis);

        MainService.getInstance().addSlide(image);
    }
}
