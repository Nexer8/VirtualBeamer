package com.virtualbeamer.utils;

import com.virtualbeamer.services.MainService;
import javafx.collections.ObservableList;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

import static com.virtualbeamer.constants.SessionConstants.*;

public class SlidesSender implements Serializable {
    private final DatagramSocket socket;

    public SlidesSender() throws SocketException {
        socket = new DatagramSocket();
    }

    public synchronized void multicast(byte[] data, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(GROUP_ADDRESS), port);
        socket.send(packet);
        MainService.getInstance().getPacketHandler().addProcessedSlide(data);
    }

    public synchronized void unicast(ObservableList<BufferedImage> slides, InetAddress address) throws IOException {
        try (Socket socket = new Socket(address, INDIVIDUAL_SLIDES_PORT)) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.writeInt(slides.size());
            for (int i = 0; i < slides.size(); i++) {
                var packets = PacketCreator.createPackets(slides.get(i), i);
                out.writeInt(packets.size());
                for (var packet : packets) {
                    out.writeInt(packet.length);
                    out.write(packet);
                }
                System.out.println("Slide " + i + " sent!");
            }
        }
    }

}
