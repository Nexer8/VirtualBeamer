package com.virtualbeamer.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketCreator {
    public static final int HEADER_SIZE = 8;
    public static final int SESSION_START = 128;
    public static final int SESSION_END = 64;
    public static final int MAX_PACKET_SIZE = 60000;

    public static final String OUTPUT_FORMAT = "jpeg";

    public static byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        baos.flush();
        byte[] temp = baos.toByteArray();
        baos.close();
        return temp;
    }

    public static List<byte[]> createPackets(BufferedImage image, int sessionNumber) throws IOException {
        byte[] imageByteArray = bufferedImageToByteArray(image, OUTPUT_FORMAT);
        int packets = (int) Math.ceil((float) imageByteArray.length / (float) MAX_PACKET_SIZE);
        List<byte[]> packetsList = new ArrayList<>();
        for (int i = 0; i <= packets; i++) {
            int flags = 0;
            flags = i == 0 ? flags | SESSION_START : flags;
            flags = (i + 1) * MAX_PACKET_SIZE > imageByteArray.length ? flags | SESSION_END : flags;

            int size = (flags & SESSION_END) != SESSION_END ? MAX_PACKET_SIZE
                    : imageByteArray.length - i * MAX_PACKET_SIZE;

            byte[] data = new byte[HEADER_SIZE + size];
            data[0] = (byte) flags;
            data[1] = (byte) sessionNumber;
            data[2] = (byte) packets;
            data[3] = (byte) (MAX_PACKET_SIZE >> 8);
            data[4] = (byte) MAX_PACKET_SIZE;
            data[5] = (byte) i;
            data[6] = (byte) (size >> 8);
            data[7] = (byte) size;

            System.arraycopy(imageByteArray, i * MAX_PACKET_SIZE, data, HEADER_SIZE, size);
            packetsList.add(data);
            if ((flags & SESSION_END) == SESSION_END) {
                break;
            }
        }
        return packetsList;
    }
}
