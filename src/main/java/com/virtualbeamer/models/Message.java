package com.virtualbeamer.models;

import com.virtualbeamer.constants.MessageType;
import com.virtualbeamer.constants.AppConstants;
import com.virtualbeamer.services.MainService;

import java.io.*;
import java.net.InetAddress;
import java.util.Collections;

public class Message implements Serializable {
    final public MessageType type;
    public GroupSession session;
    public int intVariable;
    public String stringVariable;
    public InetAddress ipAddress;
    public int packetID;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, GroupSession session) {
        this.type = type;
        this.session = session;
    }

    public Message(MessageType type, int intVariable) {
        this.type = type;
        this.intVariable = intVariable;
    }

    public Message(MessageType type, String stringVariable) {
        this.type = type;
        this.stringVariable = stringVariable;
    }

    public Message(MessageType type, GroupSession session, String stringVariable) {
        this.type = type;
        this.session = session;
        this.stringVariable = stringVariable;
    }

    public Message(MessageType type, String stringVariable, InetAddress ipAddress) {
        this.type = type;
        this.stringVariable = stringVariable;
        this.ipAddress = ipAddress;
    }

    public Message(MessageType type, String stringVariable, int intVariable, InetAddress ipAddress) {
        this.type = type;
        this.stringVariable = stringVariable;
        this.intVariable = intVariable;
        this.ipAddress = ipAddress;
    }

    public Message(MessageType type, GroupSession session, String stringVariable, InetAddress ipAddress) {
        this.type = type;
        this.session = session;
        this.stringVariable = stringVariable;
        this.ipAddress = ipAddress;
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
