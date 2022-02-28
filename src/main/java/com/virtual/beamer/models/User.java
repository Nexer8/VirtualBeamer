package com.virtual.beamer.models;

import com.virtual.beamer.constants.AppConstants;
import com.virtual.beamer.constants.MessageType;
import com.virtual.beamer.constants.SessionConstants;
import com.virtual.beamer.controllers.PresentationViewController;
import com.virtual.beamer.utils.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.virtual.beamer.constants.AppConstants.UserType.PRESENTER;
import static com.virtual.beamer.constants.AppConstants.UserType.VIEWER;
import static com.virtual.beamer.constants.SessionConstants.*;
import static com.virtual.beamer.models.Message.deserializeMessage;
import static com.virtual.beamer.models.Message.handleMessage;
import static com.virtual.beamer.utils.PacketCreator.createPackets;

public class User {

    private static volatile User instance;
    private ObservableList<BufferedImage> slides = FXCollections.observableArrayList();
    private String username;
    private int currentSlide = 0;
    private AppConstants.UserType userType = VIEWER;
    private PresentationViewController pvc;
    private final ObservableList<GroupSession> groupSessions = FXCollections.observableArrayList();
    private final ObservableList<String> groupSessionsInfo = FXCollections.observableArrayList();
    private final ObservableList<String> participantsNames = FXCollections.observableArrayList();
    private final Map<String, InetAddress> participantsInfo = new HashMap<>();
    private final Session session;
    private GroupSession groupSession;
    private final SlidesSender slidesSender;
    private final ArrayList<Integer> groupPortList = new ArrayList<>();
    private GroupReceiver gr;
    private boolean electSent = false;
    private Timer electionTimer;
    private Timer agreementTimer;
    private Timer crashDetectionTimer;
    private Map<Integer, Timer> nackTimer;
    private boolean agreementMessageSent = false;
    private int ID;
    private final ArrayList<Integer> groupIDs = new ArrayList<>();
    private CrashDetection crashDetectionThread;

    private User() throws IOException {
        MulticastReceiver mr = new MulticastReceiver();
        mr.start();

        MessageReceiver rec = new MessageReceiver();
        rec.start();

        SlidesReceiver sr = new SlidesReceiver();
        sr.start();
        IndividualSlidesReceiver isr = new IndividualSlidesReceiver();
        isr.start();

        session = new Session();
        groupSession = new GroupSession("");
        slidesSender = new SlidesSender();
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
        setID(0);
        groupSession.setName(sessionName);
        session.multicast(new Message(MessageType.COLLECT_PORTS));
        this.groupPortList.clear();

        try (DatagramSocket socket = new DatagramSocket(UNICAST_COLLECT_PORTS_PORT)) {
            byte[] buffer = new byte[10000];
//                TODO: add a constant with the timeout value
            socket.setSoTimeout(1000);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                InetAddress senderAddress = packet.getAddress();

                Message message = deserializeMessage(buffer);
                //if(message.type == MessageType.SEND_SESSION_PORT)
                handleMessage(message, senderAddress);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No ports received.");
        }

        int groupPort;
        if (groupPortList.isEmpty()) {
            groupPort = SessionConstants.STARTING_GROUP_PORT;
        } else {
            groupPort = Collections.max(groupPortList) + 1;
        }

        System.out.println("Received port:" + groupPort);
        groupSession.setPort(groupPort);
        System.out.println("Username: " + username);
        groupSession.setLeaderData(username, Helpers.getInetAddress());
        gr = new GroupReceiver(groupPort);
        gr.start();
        multicastSessionDetails();
    }

    public void joinSession(String name) throws IOException {
        this.groupIDs.clear();
        groupSession = getGroupSession(name);
        groupSession.setPort(getGroupSession(name).getPort());
        System.out.println("Test print: " + groupSession.getLeaderInfo() + " " + getGroupSession(name).getPort());
        gr = new GroupReceiver(getGroupSession(name).getPort());
        gr.start();
        groupSession.sendGroupMessage(new Message(MessageType.JOIN_SESSION,
                username, Helpers.getInetAddress()));

        // Collects IDs
        try (DatagramSocket socket = new DatagramSocket(UNICAST_SEND_USER_DATA_PORT)) {
            byte[] buffer = new byte[10000];
//                TODO: add a constant with the timeout value
            socket.setSoTimeout(1000);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                InetAddress senderAddress = packet.getAddress();

                Message message = deserializeMessage(buffer);
                handleMessage(message, senderAddress);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No ids received.");
        }

        int id = 0;
        if (!this.groupIDs.isEmpty()) {
            Collections.sort(groupIDs);
            id = groupIDs.get(groupIDs.size() - 1) + 1;
        } else
            id = 1;


        setID(id);
        System.out.println("ID set: " + id);
        crashDetectionThread = new CrashDetection();
        crashDetectionThread.start();
    }

    public void sendUserData(InetAddress senderAddress) throws IOException {
        session.sendMessage(new Message(MessageType.SEND_USER_DATA,
                username, getID(), Helpers.getInetAddress()), senderAddress, UNICAST_SEND_USER_DATA_PORT);
    }

    public void setGroupLeader(String name) throws IOException {
        userType = VIEWER;
        session.multicast(new Message(MessageType.COORD,
                groupSession, name, participantsInfo.get(name)));
    }

    private void cleanUpSessionData() {
        participantsNames.clear();
        slides.clear();
        currentSlide = 0;
        userType = VIEWER;
        gr.stop();
    }

    public void leaveSession() throws IOException {
        groupSession.sendGroupMessage(new Message(MessageType.LEAVE_SESSION, username));
        cleanUpSessionData();
    }

    public void sendHelloMessage() throws IOException {
        session.multicast(new Message(MessageType.HELLO));
    }

    public void multicastSlide() throws IOException {
        for (var packet : createPackets(slides.get(currentSlide), currentSlide)) {
            slidesSender.multicast(packet);
        }
    }

    public void sendSlides(InetAddress senderAddress) throws IOException, InterruptedException {
        for (int i = 0; i <= currentSlide; i++) {
            for (var packet : createPackets(slides.get(i), i)) {
                TimeUnit.MILLISECONDS.sleep(50);
                slidesSender.sendMessage(packet, senderAddress);
            }
        }
    }

    public void multicastSessionDetails() throws IOException {
        session.multicast(new Message(MessageType.SESSION_DETAILS, groupSession));
    }

    public void sendSessionDetails(InetAddress senderAddress) throws IOException {
        session.sendMessage(new Message(MessageType.SESSION_DETAILS, groupSession), senderAddress);
    }

    public void multicastDeleteSession() throws IOException {
        session.multicast(new Message(MessageType.DELETE_SESSION, groupSession));
        cleanUpSessionData();
    }

    public void multicastNextSlide() throws IOException {
        currentSlide++;
        multicastSlide();
    }

    public void multicastPreviousSlide() throws IOException {
        currentSlide--;
        groupSession.sendGroupMessage(new Message(MessageType.PREVIOUS_SLIDE));
    }

    public void sendGroupPort(InetAddress senderAddress) throws IOException {
        if (groupSession.getPort() != 0) {
            session.sendMessage(new Message(MessageType.SEND_SESSION_PORT,
                    groupSession.getPort()), senderAddress, UNICAST_COLLECT_PORTS_PORT);
        }
    }

    public int getCurrentSlide() {
        return currentSlide;
    }

    public ObservableList<BufferedImage> getSlides() {
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

    public void setSlides(BufferedImage[] slides) throws IOException {
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

    public void addParticipant(String name, InetAddress ipAddress) {
        participantsInfo.put(name, ipAddress);
        Platform.runLater(() -> participantsNames.add(name));
    }

    public void deleteParticipant(String name) {
        participantsInfo.remove(name);
        System.out.println(name);
        Platform.runLater(() -> participantsNames.remove(name));
    }

    public void addSessionData(GroupSession session) {
        groupSessions.add(session);
        Platform.runLater(() -> groupSessionsInfo.add(session.getName() + ": " + session.getLeaderInfo()));
        System.out.println(session.getName());
    }

    public void updateSessionData(GroupSession session, String leaderName, InetAddress addressIP) {
        if (leaderName.equals(username)) {
            userType = PRESENTER;
        }

        groupSessions.get(groupSessions.indexOf(session)).setLeaderData(leaderName, addressIP);
        if (groupSession.equals(session)) {
            groupSession.setLeaderData(leaderName, addressIP);
        }
        Platform.runLater(() -> {
            groupSessionsInfo.remove(session.getName() + ": " + session.getLeaderInfo());
            groupSessionsInfo.add(groupSessions.get(
                    groupSessions.indexOf(session)).getName() + ": " +
                    groupSessions.get(groupSessions.indexOf(session)).getLeaderInfo());
        });
        pvc.changePresenterData(groupSessions.get(groupSessions.indexOf(session)).getLeaderInfo());
    }

    public ObservableList<String> getGroupSessionsInfo() {
        return groupSessionsInfo;
    }

    public void deleteSession(GroupSession session) {
        int idx = groupSessions.indexOf(session);
        if (idx != -1) {
            String name = groupSessions.get(idx).getName();
            groupSessions.remove(idx);

            System.out.println("Session name: " + name);
            Platform.runLater(() -> groupSessionsInfo.remove(name + ": " + session.getLeaderInfo()));
        }
    }

    public void addGroupPortToList(int groupPort) {
        groupPortList.add(groupPort);
    }

    public GroupSession getGroupSession(String name) {
        return groupSessions.stream().filter(item ->
                item.getName().equals(name)).findFirst().orElse(null);
    }

    public GroupSession getGroupSession() {
        return groupSession;
    }

    public ObservableList<String> getParticipantsNames() {
        return participantsNames;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void electLeader() throws IOException {
        if (!electSent) {
            groupSession.sendGroupMessage(new Message(MessageType.ELECT, ID));
            electSent = true;

            electionTimer = new Timer(true);
            electionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        groupSession.sendGroupMessage(new Message(MessageType.COORD,
                                groupSession, username, Helpers.getInetAddress()));
                        userType = PRESENTER;
                        electSent = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, LEADER_ELECTION_TIMEOUT);
        }
    }

    public void sendStopElection(InetAddress senderAddress) throws IOException {
        session.sendMessage(new Message(MessageType.STOP_ELECT), senderAddress);
    }

    public void stopElection() {
        electionTimer.cancel();
        electSent = false;
    }

    public void agreeOnSlidesSender(InetAddress senderAddress) throws IOException {
        if (!agreementMessageSent) {
            int mID = groupIDs.isEmpty() ? ID : Collections.min(groupIDs);
            groupSession.sendGroupMessage(new Message(MessageType.START_AGREEMENT_PROCESS, mID));
            agreementMessageSent = true;

            agreementTimer = new Timer(true);
            agreementTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (ID == mID) {
                            sendSlides(senderAddress);
                        }
                        agreementMessageSent = false;
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, AGREEMENT_PROCESS_TIMEOUT);
        }
    }

    public void sendStopAgreementProcess(InetAddress senderAddress) throws IOException {
        session.sendMessage(new Message(MessageType.STOP_AGREEMENT_PROCESS), senderAddress);
    }

    public void stopAgreementProcess() {
        agreementTimer.cancel();
        agreementMessageSent = false;
    }

    public void addListGroupID(int id) {
        groupIDs.add(id);
    }

    public void sendCrashDetectionCheck(int delay) throws IOException {
        crashDetectionTimer = new Timer(false);
        System.out.println("Sending crash detection message in " + delay);

        crashDetectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    User.getInstance().groupSession.sendGroupMessage(new Message(MessageType.CRASH_DETECT, getUsername()));

                    boolean leaderCrashed = true;

                    try (DatagramSocket socket = new DatagramSocket(UNICAST_IM_ALIVE_PORT)) {
                        byte[] buffer = new byte[10000];
//                TODO: add a constant with the timeout value
                        socket.setSoTimeout(1000);
                        while (true) {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            socket.receive(packet);
                            InetAddress senderAddress = packet.getAddress();

                            Message message = deserializeMessage(buffer);
                            handleMessage(message, senderAddress);
                            leaderCrashed = false;
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("Stop waiting for IM ALIVE.");
                    }

                    if (leaderCrashed) {
                        System.out.println("Leader crashed.");
                        electLeader();
                    } else {
                        System.out.println("Leader online.");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, delay);
    }

    public void stopCrashDetectionTimer() {
        crashDetectionTimer.cancel();
    }

    public void stopCrashChecking() {
        crashDetectionThread.stop();
    }

    public void startCrashChecking() {
        crashDetectionThread.start();
    }

    public void sendImAlive(InetAddress address) throws IOException {
        session.sendMessage(new Message(MessageType.IM_ALIVE), address, UNICAST_IM_ALIVE_PORT);
    }

    public static class CrashDetection extends Thread {
        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            while (true) {
                try {
                    int delay = CRASH_DETECTION_LOWER_BOUND_TIMEOUT + (int) (Math.random() * 1000);
                    User.getInstance().sendCrashDetectionCheck(delay);
                    sleep((long) delay + 1500);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendNACKPacket(int packetID) throws IOException {
        Timer nackTimerTmp = new Timer(true);
        nackTimer.put(packetID, nackTimerTmp);

        nackTimerTmp.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    groupSession.sendGroupMessage(new Message(MessageType.NACK_PACKET, packetID));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, (int) (Math.random() * 1000));
    }

    public void stopNackTimer(int packetID) {
        nackTimer.get(packetID).cancel();
        nackTimer.remove(packetID);
    }

    public void resendPacket(int packetID) throws IOException {
        groupSession.sendGroupMessage(packetID);
    }

    public ArrayList<Integer> getGroupIDs() {
        return groupIDs;
    }
}
