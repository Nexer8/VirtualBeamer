package com.virtual.beamer.utils;

import com.virtual.beamer.models.Message;
import com.virtual.beamer.models.User;

import java.io.*;
import java.net.*;

import static com.virtual.beamer.constants.AppConstants.UserType.PRESENTER;
import static com.virtual.beamer.constants.SessionConstants.GROUP_ADDRESS;
import static com.virtual.beamer.constants.SessionConstants.MULTICAST_PORT;

public class MulticastReceiver extends Thread {
    public static final int INET_SOCKET_PORT = 1234;

    final private MulticastSocket socket;
    final private InetSocketAddress inetSocketAddress;
    final private NetworkInterface networkInterface;

    public MulticastReceiver() throws IOException {
        socket = new MulticastSocket(MULTICAST_PORT);
//        TODO: uncomment when ready
//        socket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
        inetSocketAddress = new InetSocketAddress(GROUP_ADDRESS, INET_SOCKET_PORT);
        networkInterface = NetworkInterface.getByIndex(0);
    }

    private void handleMessage(Message message) throws IOException {
        System.out.println(message.type.name());

        switch (message.type) {
            case DELETE_SESSION -> User.getInstance().deleteSession(message.session);
            case HELLO -> {
                if (User.getInstance().getUserType() == PRESENTER) {
//                    TODO: Just respond to the user that sent the hello packet!
                    User.getInstance().sendSessionDetails();
                }
            }
            case SEND_SLIDES -> {
                if (User.getInstance().getUserType() != PRESENTER) {
                    User.getInstance().setSlides(message.slides);
                }
            }
            case NEXT_SLIDE -> {
                if (User.getInstance().getUserType() != PRESENTER) {
                    User.getInstance().setCurrentSlide(User.getInstance().getCurrentSlide() + 1);
                }
            }
            case PREVIOUS_SLIDE -> {
                if (User.getInstance().getUserType() != PRESENTER) {
                    User.getInstance().setCurrentSlide(User.getInstance().getCurrentSlide() - 1);
                }
            }
            case SESSION_DETAILS -> User.getInstance().addSessionData(message.session);
        }
    }

    private Message deserializeMessage(byte[] buffer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));

        return (Message) is.readObject();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            byte[] buffer = new byte[10000];
            socket.joinGroup(inetSocketAddress, networkInterface);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Message message = deserializeMessage(buffer);
                handleMessage(message);
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
