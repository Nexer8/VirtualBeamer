package com.virtualbeamer.utils;

import com.virtualbeamer.constants.AppConstants;
import com.virtualbeamer.models.Message;
import com.virtualbeamer.services.MainService;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.time.Instant;
import java.util.Collections;

public class MessageHandler {
    public static void collectAndProcessMultipleMessages(DatagramSocket socket, byte[] buffer)
            throws IOException, ClassNotFoundException {
        // noinspection InfiniteLoopStatement
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            InetAddress senderAddress = packet.getAddress();

            Message message = deserializeMessage(buffer);
            handleMessage(message, senderAddress);
        }
    }

    public static void collectAndProcessMultipleUnicastMessages(ServerSocket serverSocket)
            throws IOException, ClassNotFoundException {
        // noinspection InfiniteLoopStatement
        while (true) {
            collectAndProcessUnicastMessage(serverSocket);
        }
    }

    public static void collectAndProcessUnicastMessage(ServerSocket serverSocket)
            throws IOException, ClassNotFoundException {
        Socket socket = serverSocket.accept();
        System.out.println("Client connected!");

        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        Message message = (Message) in.readObject();
        handleMessage(message, socket.getInetAddress());
    }

    public static Message deserializeMessage(byte[] buffer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));

        return (Message) is.readObject();
    }

    public static void handleMessage(Message message, InetAddress senderAddress) throws IOException {
        System.out.println(message.type.name());

        switch (message.type) {
            case DELETE_SESSION -> {
                if (MainService.getInstance().getGroupSession().equals(message.session)) {
                    MainService.getInstance().closeSession();
                }
                MainService.getInstance().deleteSession(message.session);
            }
            case HELLO -> {
                if (MainService.getInstance().getUserType() == AppConstants.UserType.PRESENTER) {
                    MainService.getInstance().sendSessionDetails(senderAddress);
                }
            }
            case CURRENT_SLIDE_NUMBER, NEXT_SLIDE, PREVIOUS_SLIDE -> {
                if (MainService.getInstance().getUserType() != AppConstants.UserType.PRESENTER) {
                    if (MainService.getInstance().getSlides().size() < message.intVariable + 1) {
                        MainService.getInstance().setCurrentSlide(message.intVariable);
                    } else {
                        System.out.println("Slide number is out of bounds!");
                    }
                }
            }
            case SESSION_DETAILS -> MainService.getInstance().addSessionData(message.session);
            case JOIN_SESSION -> {
                System.out.println(message.stringVariable + " joined the session.");
                if (MainService.getInstance().getParticipantsNames().isEmpty()) {
                    MainService.getInstance().sendSlides(senderAddress);
                }
                MainService.getInstance().addParticipant(message.stringVariable, message.intVariable, message.ipAddress);
                MainService.getInstance().addListGroupID(message.intVariable);
                MainService.getInstance().multicastNewParticipant(message.stringVariable, message.intVariable, message.ipAddress);
            }
            case NEW_PARTICIPANT -> {
                if (MainService.getInstance().getUserType() == AppConstants.UserType.VIEWER) {
                    MainService.getInstance().addParticipant(message.stringVariable, message.intVariable, message.ipAddress);
                    MainService.getInstance().addListGroupID(message.intVariable);

                    if (MainService.getInstance().getSlides() != null
                            && !MainService.getInstance().getSlides().isEmpty()) {
                        MainService.getInstance().agreeOnSlidesSender(senderAddress);
                    }
                }
            }
            case COLLECT_USERS_DATA -> MainService.getInstance().sendUsersData(senderAddress);
            case USER_DATA -> {
                MainService.getInstance().addParticipant(message.stringVariable, message.intVariable, message.ipAddress);
                MainService.getInstance().addListGroupID(message.intVariable);
            }
            case LEAVE_SESSION -> MainService.getInstance().deleteParticipant(message.stringVariable);
            case COORD -> {
                MainService.getInstance().updatePreviousLeaderIP(message.ipAddress.getHostAddress());
                MainService.getInstance().updateSessionData(
                        message.session, message.stringVariable, message.ipAddress, message.intVariable, true);
                MainService.getInstance().startCrashDetection();
            }
            case ELECT -> {
                MainService.getInstance().stopCrashDetection();
                MainService.getInstance().deleteParticipant(MainService.getInstance().getCurrentLeaderName());
                if (MainService.getInstance().getID() < message.intVariable) {
                    MainService.getInstance().sendStopElection(senderAddress);
                }
            }
            case STOP_ELECT -> MainService.getInstance().stopElection();
            case START_AGREEMENT_PROCESS -> {
                int mID = MainService.getInstance().getGroupIDs().isEmpty() ? MainService.getInstance().getID()
                        : Collections.min(MainService.getInstance().getGroupIDs());

                if (mID < message.intVariable) {
                    MainService.getInstance().sendStopAgreementProcess(senderAddress, message.ipAddress);
                }
            }
            case STOP_AGREEMENT_PROCESS -> MainService.getInstance().stopAgreementProcess(message.ipAddress);
            case IM_ALIVE -> {
                Instant instant = Instant.now();
                MainService.getInstance().setLastImAlive(instant.getEpochSecond());
            }
            case CHANGE_LEADER -> {
                MainService.getInstance().updatePreviousLeaderIP(message.ipAddress.getHostAddress());
                MainService.getInstance().updateSessionData(
                        message.session, message.stringVariable, message.ipAddress, message.intVariable, false);
            }
            case PASS_LEADERSHIP -> {
                MainService.getInstance().stopCrashDetection();
                MainService.getInstance().updatePreviousLeaderIP(message.ipAddress.getHostAddress());
                MainService.getInstance().updateSessionData(
                        message.session, message.stringVariable, message.ipAddress, message.intVariable, false);
                MainService.getInstance().multicastNewLeader(MainService.getInstance().getUsername());
            }
            case MESSAGE_RESEND -> {
                System.out.println("Resend packet " + message.intVariable);
                MainService.getInstance().getPacketHandler().resendMessage(senderAddress, message.intVariable);
            }
            case SLIDE_RESEND -> {
                System.out.println("Resend slide session:" + message.shortVariable1 + " slice: " + message.shortVariable2);
                MainService.getInstance().getPacketHandler().resendSlide(senderAddress, message.shortVariable1, message.shortVariable2);
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + message.type);
        }
    }
}
