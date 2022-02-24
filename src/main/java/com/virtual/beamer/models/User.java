package com.virtual.beamer.models;

import com.virtual.beamer.constants.AppConstants;
import com.virtual.beamer.constants.MessageType;
import com.virtual.beamer.constants.SessionConstants;
import com.virtual.beamer.controllers.PresentationViewController;
import com.virtual.beamer.utils.GroupReceiver;
import com.virtual.beamer.utils.MessageReceiver;
import com.virtual.beamer.utils.MulticastReceiver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;

import static com.virtual.beamer.constants.AppConstants.UserType.PRESENTER;
import static com.virtual.beamer.constants.AppConstants.UserType.VIEWER;
import static com.virtual.beamer.models.Message.deserializeMessage;
import static com.virtual.beamer.models.Message.handleMessage;

public class User {
    private static volatile User instance;
    private ObservableList<File> slides;
    private int currentSlide = 0;
    private AppConstants.UserType userType = VIEWER;
    private PresentationViewController pvc;
    private final ObservableList<GroupSession> groupSessions = FXCollections.observableArrayList();
    private final ObservableList<String> groupSessionNames = FXCollections.observableArrayList();
    private final Session session;
    private final GroupSession groupSession;
    private final ArrayList<Integer> groupPortList = new ArrayList<>();

    private User() throws IOException {
        MulticastReceiver mr = new MulticastReceiver();
        mr.start();

        MessageReceiver rec = new MessageReceiver();
        rec.start();
        session = new Session();
        groupSession = new GroupSession("");
    }

    public static User getInstance() throws IOException {
        if (instance != null) {
            return instance;
        }
        synchronized (User.class) {
            if (instance == null) {
                instance = new User();
                instance.sendHelloMessage();
            }
            return instance;
        }
    }

    public void createSession(String sessionName) throws IOException {
        userType = PRESENTER;
        groupSession.setName(sessionName);
        session.multicast(new Message(MessageType.COLLECT_PORTS));
        this.groupPortList.clear();

//        TODO: Specify port
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] buffer = new byte[10000];
                socket.setSoTimeout(5000);
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    InetAddress senderAddress = packet.getAddress();

                    Message message = deserializeMessage(buffer);
                    handleMessage(message, senderAddress);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("No ports received.");
            }
        }).start();

        int groupPort;
        if (groupPortList.isEmpty())
            groupPort = SessionConstants.STARTING_GROUP_PORT;
        else
            groupPort = Collections.max(groupPortList) + 1;

        groupSession.setPort(groupPort);
        GroupReceiver gr = new GroupReceiver(groupPort);
        gr.start();
        multicastSessionDetails();
    }

    public void joinSession(String name) throws IOException {
        GroupReceiver gr = new GroupReceiver(getGroupSession(name).getPort());
        gr.start();
        groupSession.sendGroupMessage(new Message(MessageType.JOIN_SESSION));
    }

    public void sendHelloMessage() throws IOException {
        session.multicast(new Message(MessageType.HELLO));
    }

    public void multicastSlides() throws IOException {
        groupSession.sendGroupMessage(new Message(MessageType.SEND_SLIDES, slides.toArray(new File[]{})));
    }

    public void multicastSessionDetails() throws IOException {
        session.multicast(new Message(MessageType.SESSION_DETAILS, groupSession));
    }

    public void sendSessionDetails(InetAddress senderAddress) throws IOException {
        session.sendMessage(new Message(MessageType.SESSION_DETAILS, groupSession), senderAddress);
    }

    public void multicastDeleteSession() throws IOException {
        session.multicast(new Message(MessageType.DELETE_SESSION, groupSession));
        userType = VIEWER;
    }

    public void multicastNextSlide() throws IOException {
        currentSlide++;
        groupSession.sendGroupMessage(new Message(MessageType.NEXT_SLIDE));
    }

    public void multicastPreviousSlide() throws IOException {
        currentSlide--;
        groupSession.sendGroupMessage(new Message(MessageType.PREVIOUS_SLIDE));
    }

    public void sendGroupPort(InetAddress senderAddress) throws IOException {
        if (groupSession.getPort() != 0)
            session.sendMessage(new Message(MessageType.SEND_SESSION_PORT, groupSession.getPort()), senderAddress);
    }

    public int getCurrentSlide() {
        return currentSlide;
    }

    public ObservableList<File> getSlides() {
        return slides;
    }

    public AppConstants.UserType getUserType() {
        return userType;
    }

    public void setCurrentSlide(int currentSlide) throws IOException {
        this.currentSlide = currentSlide;

        if (userType == VIEWER) {
            pvc.setSlide();
        }
    }

    public void setSlides(File[] slides) throws IOException {
        this.slides = FXCollections.observableArrayList(slides);

        if (userType == VIEWER) {
            if (pvc.getProgressIndicator().isVisible()) {
                pvc.getProgressIndicator().setVisible(false);
            }
            pvc.setSlide();
        }
    }

    public void setUserType(AppConstants.UserType userType) {
        this.userType = userType;
    }

    public void setPvc(PresentationViewController pvc) {
        this.pvc = pvc;
    }

    public void addSessionData(GroupSession session) {
        groupSessions.add(session);

        Platform.runLater(() -> groupSessionNames.add(session.getName()));
        System.out.println(session.getName());
    }

    public ObservableList<String> getGroupSessionNames() {
        return groupSessionNames;
    }

    public void deleteSession(GroupSession session) {
        int idx = groupSessions.indexOf(session);
        if (idx != -1) {
            String name = groupSessions.get(idx).getName();
            groupSessions.remove(idx);

            Platform.runLater(() -> groupSessionNames.remove(name));
        }
    }


    public void addGroupPortToList(int groupPort) {
        groupPortList.add(groupPort);
    }

    public GroupSession getGroupSession(String name) {
        return groupSessions.stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
    }

    public GroupSession getGroupSession() {
        return groupSession;
    }
}
