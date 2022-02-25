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

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, File[] slides) {
        this.type = type;
        this.slides = slides;
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
                User.getInstance().addParticipant(message.stringVariable);
                User.getInstance().sendUserData(senderAddress);
            }
            case SEND_USER_DATA -> {
                System.out.println(message.stringVariable);
                User.getInstance().addParticipant(message.stringVariable);
            }
            case LEAVE_SESSION -> {
                User.getInstance().deleteParticipant(message.stringVariable);
            }
        }
    }
}
