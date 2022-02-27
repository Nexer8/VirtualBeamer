package com.virtual.beamer.models;

import com.virtual.beamer.constants.MessageType;

import java.io.*;
import java.net.InetAddress;

import static com.virtual.beamer.constants.AppConstants.UserType.PRESENTER;

public class Message implements Serializable {
    final public MessageType type;
    public File[] slides;
    public GroupSession session;
    public int intVariable;
    public String stringVariable;
    public InetAddress ipAddress;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, File[] slides, int currentSlide) {
        this.type = type;
        this.slides = slides;
        this.intVariable = currentSlide;
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

    public Message(MessageType type, String stringVariable,int intVariable, InetAddress ipAddress) {
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
            case DELETE_SESSION -> User.getInstance().deleteSession(message.session);
            case HELLO -> {
                if (User.getInstance().getUserType() == PRESENTER) {
                    User.getInstance().sendSessionDetails(senderAddress);
                }
            }
            case SEND_SLIDES -> {
                if (User.getInstance().getUserType() != PRESENTER) {
                    User.getInstance().setSlides(message.slides);
                    User.getInstance().setCurrentSlide(message.intVariable);
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
            case COLLECT_PORTS -> {
                if (User.getInstance().getUserType() == PRESENTER) {
                    User.getInstance().sendGroupPort(senderAddress);
                }
            }
            case SEND_SESSION_PORT -> User.getInstance().addGroupPortToList(message.intVariable);
            case JOIN_SESSION -> {
                if(!User.getInstance().getUsername().equals(message.stringVariable))
                {
                    System.out.println(message.stringVariable + " joined the session.");
                    User.getInstance().addParticipant(message.stringVariable, message.ipAddress);
                    User.getInstance().sendUserData(senderAddress);

                    if (!User.getInstance().getSlides().isEmpty()) {
                        User.getInstance().agreeOnSlidesSender(senderAddress);
                    }
                }

            }
            case SEND_USER_DATA -> {

                System.out.println(User.getInstance().getUsername()+ " added " + message.stringVariable + " to participants list.");
                User.getInstance().addParticipant(message.stringVariable, message.ipAddress);
                User.getInstance().addListGroupID(message.intVariable);
            }
            case LEAVE_SESSION -> User.getInstance().deleteParticipant(message.stringVariable);
            case COORD -> User.getInstance().updateSessionData(
                    message.session, message.stringVariable, message.ipAddress);
            case ELECT -> {
                if (User.getInstance().getID() < message.intVariable) {
                    User.getInstance().sendStopElection(senderAddress);
                }
            }
            case STOP_ELECT -> User.getInstance().stopElection();
            case START_AGREEMENT_PROCESS -> {
                if (User.getInstance().getID() < message.intVariable) {
                    User.getInstance().sendStopAgreementProcess(senderAddress);
                }
            }
            case STOP_AGREEMENT_PROCESS -> User.getInstance().stopAgreementProcess();
        }
    }
}
