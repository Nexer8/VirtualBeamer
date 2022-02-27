package com.virtual.beamer.models;

import com.virtual.beamer.utils.Helpers;

import java.io.*;
import java.net.*;

import static com.virtual.beamer.constants.SessionConstants.*;

public class Session implements Serializable {
    private final DatagramSocket socket;

    public Session() throws SocketException {
        socket = new DatagramSocket();
    }

    public void multicast(Message message) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length,
                InetAddress.getByName(GROUP_ADDRESS), MULTICAST_PORT);
        socket.send(packet);
    }

    public void sendMessage(Message message, InetAddress address, int port) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(6400);
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        final byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        System.out.println("Responded to Hello message!");
    }

    public void sendMessage(Message message, InetAddress address) throws IOException {
        sendMessage(message, address, INDIVIDUAL_MESSAGE_PORT);
    }

    public void sendFiles(Message message, InetAddress senderAddress) throws IOException {
        Socket socket = new Socket(Helpers.getInetAddress(), 9999);

        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(message.slides.length);

        for (File file : message.slides) {
            long length = file.length();
            dos.writeLong(length);

            String name = file.getName();
            dos.writeUTF(name);

            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            int theByte = 0;
            while ((theByte = bis.read()) != -1) bos.write(theByte);

            bis.close();
        }

        dos.close();
    }

    public void receiveFiles(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();

        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
        DataInputStream dis = new DataInputStream(bis);

        int filesCount = dis.readInt();
        File[] files = new File[filesCount];

        for (int i = 0; i < filesCount; i++) {
            long fileLength = dis.readLong();
            String fileName = dis.readUTF();

            files[i] = new File("temp" + "/" + fileName);

            FileOutputStream fos = new FileOutputStream(files[i]);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            for (int j = 0; j < fileLength; j++) bos.write(bis.read());

            bos.close();
        }

        dis.close();
    }
}
