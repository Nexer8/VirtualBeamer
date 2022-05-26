package com.virtualbeamer.services;

import com.virtualbeamer.constants.AppConstants;
import com.virtualbeamer.controllers.PresentationViewController;
import com.virtualbeamer.models.GlobalSession;
import com.virtualbeamer.models.GroupSession;
import com.virtualbeamer.models.Message;
import com.virtualbeamer.models.User;
import com.virtualbeamer.receivers.*;
import com.virtualbeamer.utils.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.virtualbeamer.constants.MessageType.*;
import static com.virtualbeamer.constants.SessionConstants.*;
import static com.virtualbeamer.utils.MessageHandler.collectAndProcessUnicastMessage;

public class MainService {
    private static volatile MainService instance;
    private final User user = new User();

    private ObservableList<BufferedImage> slides = FXCollections.observableArrayList();
    private int currentSlide = 0;

    private PresentationViewController pvc;

    private final ObservableList<GroupSession> groupSessions = FXCollections.observableArrayList();
    private final ObservableList<String> groupSessionsInfo = FXCollections.observableArrayList();
    private final ObservableList<String> participantsNames = FXCollections.observableArrayList();
    private final Map<String, InetAddress> participantsInfo = new HashMap<>();
    private final ArrayList<Integer> groupIDs = new ArrayList<>();
    private final ArrayList<Integer> groupPortList = new ArrayList<>();

    private final GlobalSession globalSession;
    private GroupSession groupSession;
    private GroupReceiver groupReceiver;
    private final SlidesSender slidesSender;
    private SlidesReceiver slidesReceiver;
    private final Map<InetAddress, Boolean> agreementMessageSent = new HashMap<>();
    private final Map<InetAddress, Timer> agreementTimer = new HashMap<>();

    private final Map<Integer, Timer> nackTimer = new HashMap<>();
    private CrashDetection crashDetection;

    private long lastImAlive;

    private MainService() throws IOException {
        MulticastReceiver multicastReceiver = new MulticastReceiver();
        multicastReceiver.start();

        UnicastReceiver unicastReceiver = new UnicastReceiver();
        unicastReceiver.start();

        globalSession = new GlobalSession();
        groupSession = new GroupSession("");

        IndividualSlidesReceiver individualSlidesReceiver = new IndividualSlidesReceiver();
        individualSlidesReceiver.start();

        slidesSender = new SlidesSender();
        lastImAlive = 0;
    }

    public static MainService getInstance() throws IOException {
        if (instance != null) {
            return instance;
        }
        synchronized (MainService.class) {
            if (instance == null) {
                instance = new MainService();

                Runnable sendHelloMessage = () -> {
                    try {
                        instance.cleanUpSessionsData();
                        instance.sendHelloMessage();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(sendHelloMessage, 0, HELLO_MESSAGE_PERIODICITY, TimeUnit.SECONDS);

            }
            return instance;
        }
    }

    public void createSession(String sessionName) throws IOException {
        user.setUserType(AppConstants.UserType.PRESENTER);
        user.setID(0);
        groupSession.setName(sessionName);
        globalSession.multicast(new Message(COLLECT_PORTS));
        this.groupPortList.clear();

        try (ServerSocket socket = new ServerSocket(UNICAST_COLLECT_PORTS_PORT)) {
            socket.setSoTimeout(SO_TIMEOUT);
            collectAndProcessUnicastMessage(socket);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No ports received.");
        }

        int groupPort;
        if (groupPortList.isEmpty()) {
            groupPort = STARTING_GROUP_PORT;
        } else {
            groupPort = Collections.max(groupPortList) + 1;
        }

        System.out.println("Received port:" + groupPort);
        groupSession.setPort(groupPort);
        System.out.println("Username: " + user.getUsername());
        groupSession.setLeaderData(user.getUsername(), Helpers.getInetAddress());
        groupSession.updatePreviousLeaderIpAddress();
        groupReceiver = new GroupReceiver(groupPort);
        groupReceiver.start();

        multicastSessionDetails();
        startCrashDetection();
    }

    public void joinSession(String name) throws IOException {
        this.groupIDs.clear();
        groupSession = getGroupSession(name);
        groupSession.setPort(getGroupSession(name).getPort());
        System.out.println("Leader: " + groupSession.getLeaderInfo() + " " + getGroupSession(name).getPort());
        groupReceiver = new GroupReceiver(getGroupSession(name).getPort());
        groupReceiver.start();

        slidesReceiver = new SlidesReceiver(getGroupSession(name).getPort());
        slidesReceiver.start();

        groupSession.sendGroupMessage(new Message(JOIN_SESSION,
                user.getUsername(), Helpers.getInetAddress()));

        // Collect IDs
        try (ServerSocket socket = new ServerSocket(UNICAST_SEND_USER_DATA_PORT)) {
            socket.setSoTimeout(SO_TIMEOUT);
            collectAndProcessUnicastMessage(socket);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No ids received.");
        }

        int id = 0;
        if (!this.groupIDs.isEmpty()) {
            Collections.sort(groupIDs);
            id = groupIDs.get(groupIDs.size() - 1) + 1;
        } else
            id = 1;

        user.setID(id);
        System.out.println("ID set: " + id);
        startCrashDetection();

        if (groupSession.getPreviousLeaderIpAddress() != null
                && groupSession.getPreviousLeaderIpAddress().equals(Helpers.getInetAddress().getHostAddress()))
            sendChangeLeader();

    }

    public void sendUserData(InetAddress senderAddress) throws IOException {
        globalSession.sendMessage(new Message(SEND_USER_DATA,
                        user.getUsername(), user.getID(), Helpers.getInetAddress()), senderAddress,
                UNICAST_SEND_USER_DATA_PORT);
    }

    public void setGroupLeader(String name) throws IOException {
        user.setUserType(AppConstants.UserType.VIEWER);
        globalSession.multicast(new Message(CHANGE_LEADER,
                groupSession, name, participantsInfo.get(name)));
    }

    private void cleanUpSessionData() {
        participantsNames.clear();
        slides.clear();
        currentSlide = 0;
        user.setUserType(AppConstants.UserType.VIEWER);
        groupReceiver.interrupt();
    }

    public void leaveSession() throws IOException {
        groupSession.sendGroupMessage(new Message(LEAVE_SESSION, user.getUsername()));
        cleanUpSessionData();
    }

    public void sendHelloMessage() throws IOException {
        globalSession.multicast(new Message(HELLO));
    }

    public void multicastSlides() throws IOException {
        for (int i = 0; i < slides.size(); i++) {
            System.out.println("Sending slide " + i);
            for (var packet : PacketCreator.createPackets(slides.get(i), i)) {
                slidesSender.multicast(packet, SLIDES_MULTICAST_BASE_PORT + groupSession.getPort());
                try {
                    Thread.sleep(200);// TODO: To delete
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        multicastCurrentSlideNumber();
    }

    public void multicastCurrentSlideNumber() throws IOException {
        groupSession.sendGroupMessage(new Message(CURRENT_SLIDE_NUMBER,
                currentSlide));
    }

    public void sendCurrentSlideNumber(InetAddress address) throws IOException {
        globalSession.sendMessage(new Message(CURRENT_SLIDE_NUMBER, currentSlide), address);
    }

    public void sendSlides(InetAddress senderAddress) throws IOException, InterruptedException {
        slidesSender.unicast(slides, senderAddress);
        sendCurrentSlideNumber(senderAddress);
    }

    public void multicastSessionDetails() throws IOException {
        globalSession.multicast(new Message(SESSION_DETAILS, groupSession));
    }

    public void sendSessionDetails(InetAddress senderAddress) throws IOException {
        globalSession.sendMessage(new Message(SESSION_DETAILS, groupSession), senderAddress);
    }

    public void multicastDeleteSession() throws IOException {
        globalSession.multicast(new Message(DELETE_SESSION, groupSession));
        cleanUpSessionData();
    }

    public void multicastNextSlide() throws IOException {
        currentSlide++;
        groupSession.sendGroupMessage(new Message(NEXT_SLIDE, currentSlide));
    }

    public void multicastPreviousSlide() throws IOException {
        currentSlide--;
        groupSession.sendGroupMessage(new Message(PREVIOUS_SLIDE, currentSlide));
    }

    public void sendGroupPort(InetAddress senderAddress) throws IOException {
        if (groupSession.getPort() != 0) {
            globalSession.sendMessage(new Message(SEND_SESSION_PORT,
                    groupSession.getPort()), senderAddress, UNICAST_COLLECT_PORTS_PORT);
        }
    }

    public int getCurrentSlide() {
        return currentSlide;
    }

    public ObservableList<BufferedImage> getSlides() {
        return slides;
    }

    public synchronized void addSlide(BufferedImage slide) {
        slides.add(slide);
    }

    public AppConstants.UserType getUserType() {
        return user.getUserType();
    }

    public synchronized void setCurrentSlide(int currentSlide) throws IOException {
        this.currentSlide = currentSlide;

        if (user.getUserType() == AppConstants.UserType.VIEWER) {
            pvc.setSlide();
        }
    }

    public void setSlides(BufferedImage[] slides) throws IOException {
        this.slides = FXCollections.observableArrayList(slides);

        if (user.getUserType() == AppConstants.UserType.VIEWER) {
            if (pvc.getProgressIndicator().isVisible()) {
                pvc.getProgressIndicator().setVisible(false);
            }
            pvc.setSlide();
        }
    }

    public void setUserType(AppConstants.UserType userType) {
        user.setUserType(userType);
    }

    public void setPvc(PresentationViewController pvc) {
        this.pvc = pvc;
    }

    public synchronized void addParticipant(String name, InetAddress ipAddress) {
        participantsInfo.put(name, ipAddress);
        Platform.runLater(() -> participantsNames.add(name));
    }

    public synchronized void deleteParticipant(String name) {
        participantsInfo.remove(name);
        System.out.println(name);
        Platform.runLater(() -> participantsNames.remove(name));
    }

    public synchronized void addSessionData(GroupSession session) {
        groupSessions.add(session);
        Platform.runLater(() -> groupSessionsInfo.add(session.getName() + ": " + session.getLeaderInfo()));
        System.out.println(session.getName());
    }

    public synchronized void cleanUpSessionsData() {
        groupSessions.clear();
        Platform.runLater(groupSessionsInfo::clear);
    }

    public synchronized void updateSessionData(GroupSession session, String leaderName, InetAddress addressIP) {
        if (leaderName.equals(user.getUsername())) {
            user.setUserType(AppConstants.UserType.PRESENTER);
        } else
            user.setUserType(AppConstants.UserType.VIEWER);

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

    public synchronized void deleteSession(GroupSession session) {
        int idx = groupSessions.indexOf(session);
        if (idx != -1) {
            String name = groupSessions.get(idx).getName();
            groupSessions.remove(idx);

            System.out.println("Session name: " + name);
            Platform.runLater(() -> groupSessionsInfo.remove(name + ": " + session.getLeaderInfo()));
        }
    }

    public synchronized void addGroupPortToList(int groupPort) {
        groupPortList.add(groupPort);
    }

    public GroupSession getGroupSession(String name) {
        return groupSessions.stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
    }

    public GroupSession getGroupSession() {
        return groupSession;
    }

    public ObservableList<String> getParticipantsNames() {
        return participantsNames;
    }

    public String getUsername() {
        return user.getUsername();
    }

    public void setUsername(String username) {
        user.setUsername(username);
    }

    public int getID() {
        return user.getID();
    }

    public void sendElect() throws IOException {
        groupSession.sendGroupMessage(new Message(ELECT, user.getID()));
    }

    public void sendCOORD() throws IOException {
        groupSession.sendGroupMessage(new Message(COORD,
                groupSession, user.getUsername(), Helpers.getInetAddress()));
    }

    public void sendChangeLeader() throws IOException {
        globalSession.multicast(new Message(CHANGE_LEADER,
                groupSession, user.getUsername(), Helpers.getInetAddress()));
    }

    public void sendStopElection(InetAddress senderAddress) throws IOException {
        globalSession.sendMessage(new Message(STOP_ELECT), senderAddress);
    }

    public void stopElection() {
        crashDetection.stopElection();
    }

    public synchronized void agreeOnSlidesSender(InetAddress senderAddress) throws IOException {
        if (!agreementMessageSent.containsKey(senderAddress) || !agreementMessageSent.get(senderAddress)) {
            int mID = groupIDs.isEmpty() ? user.getID() : Collections.min(groupIDs);
            groupSession.sendGroupMessage(new Message(START_AGREEMENT_PROCESS, mID, senderAddress));
            agreementMessageSent.put(senderAddress, true);

            agreementTimer.put(senderAddress, new Timer(true));
            agreementTimer.get(senderAddress).schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (user.getID() == mID) {
                            sendSlides(senderAddress);
                        }
                        agreementMessageSent.put(senderAddress, false);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, AGREEMENT_PROCESS_TIMEOUT);
        }
    }

    public void sendStopAgreementProcess(InetAddress senderAddress, InetAddress viewerAddress) throws IOException {
        globalSession.sendMessage(new Message(STOP_AGREEMENT_PROCESS, viewerAddress), senderAddress);
    }

    public synchronized void stopAgreementProcess(InetAddress viewerAddress) {
        agreementTimer.get(viewerAddress).cancel();
        agreementTimer.remove(viewerAddress);
        agreementMessageSent.remove(viewerAddress);
    }

    public synchronized void addListGroupID(int id) {
        groupIDs.add(id);
    }

    public void stopCrashDetection() {
        System.out.println("Stopped crash detection");
        crashDetection.stopCrashDetectionTimer();
        crashDetection.interrupt();
        lastImAlive = 0;
    }

    public void startCrashDetection() {
        crashDetection = new CrashDetection();
        crashDetection.start();
    }

    public void sendImAlive() throws IOException {
        groupSession.sendGroupMessage(new Message(IM_ALIVE));
    }

    public synchronized void sendNACKPacket(int packetID) {
        Timer nackTimerTmp = new Timer(true);
        nackTimer.put(packetID, nackTimerTmp);

        nackTimerTmp.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    groupSession.sendGroupMessage(new Message(NACK_PACKET, packetID));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, (int) (Math.random() * 1000));
    }

    public synchronized void stopNACKTimer(int packetID) {
        nackTimer.get(packetID).cancel();
        nackTimer.remove(packetID);
    }

    public void resendPacket(int packetID) throws IOException {
        groupSession.sendGroupMessage(packetID);
    }

    public ArrayList<Integer> getGroupIDs() {
        return groupIDs;
    }

    public void setLastImAlive(long lastImAlive) {
        this.lastImAlive = lastImAlive;
    }

    public long getLastImAlive() {
        return this.lastImAlive;
    }

    public void updatePreviousLeaderIP(String leaderIPAddress) {
        groupSession.updatePreviousLeaderIpAddress(leaderIPAddress);
    }

    public String getCurrentLeaderName() {
        return this.groupSession.getLeaderName();
    }
}
