package com.virtual.beamer.utils;

import com.virtual.beamer.models.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Objects;

import static com.virtual.beamer.constants.SessionConstants.*;
import static com.virtual.beamer.utils.PacketCreator.*;

public class IndividualSlidesReceiver extends Thread {
    final private DatagramSocket socket;

    public IndividualSlidesReceiver() throws IOException {
        socket = new DatagramSocket(INDIVIDUAL_SLIDES_PORT);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            int currentSession = -1;
            int slicesStored = 0;
            int[] slicesCol = null;
            byte[] imageData = null;
            boolean sessionAvailable = false;
            byte[] buffer = new byte[DATAGRAM_MAX_SIZE + 8];

            while (true) {
                /* Receive a UDP packet */
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                byte[] data = packet.getData();

                /* Read header information */
                short session = (short) (data[1] & 0xff);
                short slices = (short) (data[2] & 0xff);
                int maxPacketSize = (int) ((data[3] & 0xff) << 8 | (data[4] & 0xff)); // mask
                // the
                // sign
                // bit
                short slice = (short) (data[5] & 0xff);
                int size = (int) ((data[6] & 0xff) << 8 | (data[7] & 0xff)); // mask

                System.out.println("Inside");
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

                if (slicesStored == slices) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(Objects.requireNonNull(imageData));
                    BufferedImage image = ImageIO.read(bis);
                    if (User.getInstance().getSlides().size() < currentSession + 1) {
                        User.getInstance().getSlides().add(image);
                    }
                    User.getInstance().setCurrentSlide(currentSession);
                    System.out.println("Image " + currentSession + " downloaded");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}
