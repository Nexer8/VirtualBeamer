package com.virtualbeamer.utils;

import com.virtualbeamer.constants.AppConstants;
import com.virtualbeamer.models.Message;
import com.virtualbeamer.models.Participant;
import com.virtualbeamer.services.MainService;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;
import java.time.Instant;
import java.util.Comparator;

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
                    while (MainService.getInstance().getSlides().size() < message.intVariable + 1) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            System.out.println("Waiting for slides to be received before setting the slide!");
                        }
                    }
                    MainService.getInstance().setCurrentSlide(message.intVariable);
                }
            }
            case SESSION_DETAILS -> MainService.getInstance().addSessionData(message.session);
            case JOIN_SESSION -> {
                System.out.println(message.participant.name + " joined the session.");
                if (MainService.getInstance().getParticipantsNames().isEmpty() &&
                        !MainService.getInstance().getSlides().isEmpty()) {
                    MainService.getInstance().sendSlides(senderAddress);
                }
                MainService.getInstance().addParticipant(message.participant);
                MainService.getInstance().multicastNewParticipant(message.participant);
            }
            case NEW_PARTICIPANT -> {
                if (MainService.getInstance().getUserType() == AppConstants.UserType.VIEWER) {
                    MainService.getInstance().addParticipant(message.participant);

                    if (MainService.getInstance().getSlides() != null
                            && !MainService.getInstance().getSlides().isEmpty()
                            && !message.ipAddress.equals(Helpers.getInetAddress())) {
                        MainService.getInstance().agreeOnSlidesSender(message.ipAddress);
                    }
                }
            }
            case COLLECT_USERS_DATA -> MainService.getInstance().sendUsersData(senderAddress);
            case USER_DATA -> MainService.getInstance().addParticipant(message.participant);
            case LEAVE_SESSION -> {
                MainService.getInstance().deleteParticipant(message.participant);
                MainService.getInstance().multicastDeleteParticipant(message.participant);
            }
            case DELETE_PARTICIPANT -> MainService.getInstance().deleteParticipant(message.participant);
            case COORD -> {
                MainService.getInstance().stopCrashDetection();
                MainService.getInstance().updatePreviousLeaderIP(message.ipAddress.getHostAddress());
                MainService.getInstance().updateSessionData(message.session, message.participant, true);
                MainService.getInstance().startCrashDetection();
            }
            case ELECT -> {
                MainService.getInstance().stopCrashDetection();
                MainService.getInstance().deleteParticipant(new Participant(
                        MainService.getInstance().getCurrentLeaderName(),
                        MainService.getInstance().getCurrentLeaderID(),
                        InetAddress.getByName(MainService.getInstance().getCurrentLeaderIP())
                ));
                if (MainService.getInstance().getID() < message.intVariable) {
                    MainService.getInstance().sendStopElection(senderAddress);
                }
            }
            case STOP_ELECT -> MainService.getInstance().stopElection();
            case START_AGREEMENT_PROCESS -> {
                int mID = MainService.getInstance().getParticipants().isEmpty() ? MainService.getInstance().getID() :
                        MainService.getInstance().getParticipants().stream().min(Comparator.comparing(v -> v.ID)).get().ID;

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
                MainService.getInstance().stopCrashDetection();
                MainService.getInstance().updatePreviousLeaderIP(message.participant.ipAddress.getHostAddress());
                MainService.getInstance().updateSessionData(message.session, message.participant, false);
                MainService.getInstance().startCrashDetection();
            }
            case PASS_LEADERSHIP -> {
                MainService.getInstance().stopCrashDetection();
                MainService.getInstance().updatePreviousLeaderIP(message.participant.ipAddress.getHostAddress());
                MainService.getInstance().updateSessionData(
                        message.session, message.participant, false);
                MainService.getInstance().multicastNewLeader(message.participant);
                MainService.getInstance().startCrashDetection();
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
