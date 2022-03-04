package com.virtualbeamer.utils;

import com.virtualbeamer.constants.AppConstants;
import com.virtualbeamer.models.Message;
import com.virtualbeamer.services.MainService;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;

public class MessageHandler {
    public static void collectAndProcessMessage(DatagramSocket socket, byte[] buffer) throws IOException, ClassNotFoundException {
        //noinspection InfiniteLoopStatement
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            InetAddress senderAddress = packet.getAddress();

            Message message = deserializeMessage(buffer);
            handleMessage(message, senderAddress);
        }
    }

    public static Message deserializeMessage(byte[] buffer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer);
        ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));

        return (Message) is.readObject();
    }

    public static void handleMessage(Message message, InetAddress senderAddress) throws IOException {
        System.out.println(message.type.name());

        switch (message.type) {
            case DELETE_SESSION -> MainService.getInstance().deleteSession(message.session);
            case HELLO -> {
                if (MainService.getInstance().getUserType() == AppConstants.UserType.PRESENTER) {
                    MainService.getInstance().sendSessionDetails(senderAddress);
                }
            }
            case CURRENT_SLIDE_NUMBER -> {
                if (MainService.getInstance().getUserType() != AppConstants.UserType.PRESENTER) {
                    MainService.getInstance().setCurrentSlide(message.intVariable);
                }
            }
            case NEXT_SLIDE -> {
                if (MainService.getInstance().getUserType() != AppConstants.UserType.PRESENTER) {
                    MainService.getInstance().setCurrentSlide(MainService.getInstance().getCurrentSlide() + 1);
                }
            }
            case PREVIOUS_SLIDE -> {
                if (MainService.getInstance().getUserType() != AppConstants.UserType.PRESENTER) {
                    MainService.getInstance().setCurrentSlide(MainService.getInstance().getCurrentSlide() - 1);
                }
            }
            case SESSION_DETAILS -> MainService.getInstance().addSessionData(message.session);
            case COLLECT_PORTS -> {
                if (MainService.getInstance().getUserType() == AppConstants.UserType.PRESENTER) {
                    MainService.getInstance().sendGroupPort(senderAddress);
                }
            }
            case SEND_SESSION_PORT -> MainService.getInstance().addGroupPortToList(message.intVariable);
            case JOIN_SESSION -> {
                if (!MainService.getInstance().getUsername().equals(message.stringVariable)) {
                    System.out.println(message.stringVariable + " joined the session.");
                    MainService.getInstance().addParticipant(message.stringVariable, message.ipAddress);
                    MainService.getInstance().sendUserData(senderAddress);

                    if (MainService.getInstance().getSlides() != null && !MainService.getInstance().getSlides().isEmpty()) {
                        MainService.getInstance().agreeOnSlidesSender(senderAddress);
                    }
                }
            }
            case SEND_USER_DATA -> {
                System.out.println(MainService.getInstance().getUsername() + " added " + message.stringVariable + " to participants list.");
                MainService.getInstance().addParticipant(message.stringVariable, message.ipAddress);
                MainService.getInstance().addListGroupID(message.intVariable);
            }
            case LEAVE_SESSION -> MainService.getInstance().deleteParticipant(message.stringVariable);
            case COORD -> {
                MainService.getInstance().updateSessionData(
                        message.session, message.stringVariable, message.ipAddress);
                MainService.getInstance().startCrashChecking();
            }
            case ELECT -> {
                MainService.getInstance().stopCrashChecking();
                if (MainService.getInstance().getID() < message.intVariable) {
                    MainService.getInstance().sendStopElection(senderAddress);
                }
            }
            case STOP_ELECT -> {
                MainService.getInstance().stopCrashChecking();

                MainService.getInstance().stopElection();
            }
            case START_AGREEMENT_PROCESS -> {
                int mID = MainService.getInstance().getGroupIDs().isEmpty() ? MainService.getInstance().getID() : Collections.min(MainService.getInstance().getGroupIDs());

                if (mID < message.intVariable) {
                    MainService.getInstance().sendStopAgreementProcess(senderAddress);
                }
            }
            case STOP_AGREEMENT_PROCESS -> MainService.getInstance().stopAgreementProcess();
            case CRASH_DETECT -> {
                if (!MainService.getInstance().getUsername().equals(message.stringVariable)) {
                    if (MainService.getInstance().getUserType() == AppConstants.UserType.VIEWER) {
                        System.out.println("Crash timer stopped.");
                        MainService.getInstance().stopCrashDetectionTimer();
                    } else
                        MainService.getInstance().sendImAlive(senderAddress);
                }
            }
            case NACK_PACKET -> {
                if (MainService.getInstance().getUserType() == AppConstants.UserType.PRESENTER)
                    MainService.getInstance().resendPacket(message.packetID);
                else
                    MainService.getInstance().stopNACKTimer(message.packetID);
            }
        }
    }
}
