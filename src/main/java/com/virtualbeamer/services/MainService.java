package com.virtualbeamer.services;

import com.virtualbeamer.constants.AppConstants;
import com.virtualbeamer.controllers.PresentationViewController;
import com.virtualbeamer.models.*;
import com.virtualbeamer.receivers.*;
import com.virtualbeamer.utils.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static com.virtualbeamer.constants.MessageType.*;
import static com.virtualbeamer.constants.SessionConstants.*;
import static com.virtualbeamer.utils.MessageHandler.collectAndProcessMultipleUnicastMessages;

public class MainService {
    private static volatile MainService instance;
    private final User user = new User();

    private ObservableList<BufferedImage> slides = FXCollections.observableArrayList();
    private int currentSlide = 0;

    private PresentationViewController pvc;

    private final ObservableList<GroupSession> groupSessions = FXCollections.observableArrayList();
    private final ObservableList<String> groupSessionsInfo = FXCollections.observableArrayList();
    private final ObservableList<Participant> participantsNames = FXCollections.observableArrayList();
    private final List<Participant> participants = new ArrayList<>();
    private final List<Participant> availabilityConfirmedParticipants = new ArrayList<>();
    private final GlobalSession globalSession;
    private GroupSession groupSession;
    private GroupReceiver groupReceiver;
    private final SlidesSender slidesSender;
    private SlidesReceiver slidesReceiver;
    private final Map<InetAddress, Boolean> agreementMessageSent = new HashMap<>();
    private final Map<InetAddress, Timer> agreementTimer = new HashMap<>();

    private static final Map<Integer, CircularFifoQueue<Boolean>> portAvailabilityHistory = new HashMap<>();
    private CrashDetection crashDetection;

    private long lastImAlive;

    private static ScheduledExecutorService executor;

    private static ScheduledFuture<?> handler;

    private PacketHandler packetHandler;

    private static volatile int helloCounter = 0;

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
        packetHandler = new PacketHandler();
        packetHandler.start();
    }

    public static void startSendingPeriodicalHELLO() {
        helloCounter = 0;
        Runnable sendHelloMessage = () -> {
            try {
                instance.cleanUpSessionsData();

                for (int port : portAvailabilityHistory.keySet()) {
                    if (!portAvailabilityHistory.get(port).get(0)
                            && !portAvailabilityHistory.get(port).get(1)
                            && !portAvailabilityHistory.get(port).get(2)) {
                        portAvailabilityHistory.remove(port);
                    }
                }
                instance.sendHelloMessage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        executor = Executors.newScheduledThreadPool(1);
        handler = executor.scheduleAtFixedRate(sendHelloMessage, 0, HELLO_MESSAGE_PERIODICITY, TimeUnit.SECONDS);
    }

    public static void stopSendingPeriodicalHELLO() {
        handler.cancel(true);
        executor.shutdown();
    }

    public static MainService getInstance() throws IOException {
        if (instance != null) {
            return instance;
        }
        synchronized (MainService.class) {
            if (instance == null) {
                instance = new MainService();
                startSendingPeriodicalHELLO();
            }
            return instance;
        }
    }

    public void createSession(String sessionName) throws IOException, InterruptedException {
        user.setUserType(AppConstants.UserType.PRESENTER);
        user.setID(0);
        groupSession.setName(sessionName);

        while (helloCounter < 3) {
            Thread.sleep(200);
        }

        int groupPort;
        if (portAvailabilityHistory.isEmpty()) {
            groupPort = STARTING_GROUP_PORT;
        } else {
            groupPort = Collections.max(portAvailabilityHistory.keySet()) + 1;
        }

        System.out.println("Received port:" + groupPort);
        groupSession.setPort(groupPort);
        System.out.println("Username: " + user.getUsername());
        groupSession.setLeaderData(new Participant(user.getUsername(), user.getID(), Helpers.getInetAddress()));
        groupSession.updatePreviousLeaderIpAddress();
        groupReceiver = new GroupReceiver(groupPort);
        groupReceiver.start();
        groupSessions.add(groupSession);
        portAvailabilityHistory.put(groupPort, getCircularFifoQueue());

        multicastSessionDetails();
        startCrashDetection();
    }

    private void collectUsersData() {
        // Collect IDs
        try (ServerSocket socket = new ServerSocket(UNICAST_SEND_USER_DATA_PORT)) {
            socket.setSoTimeout(SO_TIMEOUT);
            collectAndProcessMultipleUnicastMessages(socket);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No ids received.");
        }
    }

    public void joinSession(String name) throws IOException {
        groupSession = getGroupSession(name);
        groupSession.setPort(getGroupSession(name).getPort());
        System.out.println("Leader: " + groupSession.getLeaderInfo() + " " + getGroupSession(name).getPort());
        groupReceiver = new GroupReceiver(getGroupSession(name).getPort());
        groupReceiver.start();

        slidesReceiver = new SlidesReceiver(getGroupSession(name).getPort());
        slidesReceiver.start();

        globalSession.sendMessage(new Message(COLLECT_USERS_DATA),
                InetAddress.getByName(getGroupSession(name).getLeaderIPAddress()));

        collectUsersData();

        int id;
        if (!participants.isEmpty()) {
            id = participants.stream().max(Comparator.comparing(v -> v.ID)).get().ID + 1;
        } else {
            id = 1;
        }
        user.setID(id);
        System.out.println("ID set: " + id);

        globalSession.sendMessage(new Message(JOIN_SESSION, new Participant(user.getUsername(), user.getID(),
                Helpers.getInetAddress())), InetAddress.getByName(groupSession.getLeaderIPAddress()));

        startCrashDetection();

        if (groupSession.getPreviousLeaderIpAddress() != null
                && groupSession.getPreviousLeaderIpAddress().equals(Helpers.getInetAddress().getHostAddress())) {
            sendChangeLeader();
        }
    }

    public void sendUsersData(InetAddress senderAddress) {
        for (var participant : participants) {
            globalSession.sendMessage(new Message(USER_DATA, participant), senderAddress, UNICAST_SEND_USER_DATA_PORT);
        }
    }

    public void showParticipantUnavailableAlert(String name) {
        pvc.showParticipantUnavailableAlert(name);
    }

    public void setGroupLeader(Participant newLeader) {
        globalSession.sendMessage(new Message(PASS_LEADERSHIP, groupSession, newLeader), newLeader.ipAddress);
        if (participants.contains(newLeader)) {
            System.out.println("Leader: " + newLeader.name);
            System.out.println("ID: " + newLeader.ID);
            System.out.println("IP: " + newLeader.ipAddress);
            stopCrashDetection();
            user.setUserType(AppConstants.UserType.VIEWER);
        }
    }

    public void multicastNewLeader(Participant newLeader) throws IOException {
        globalSession.multicast(new Message(CHANGE_LEADER, groupSession, newLeader));
    }

    private void cleanUpSessionData() {
        participantsNames.clear();
        participants.clear();
        slides.clear();
        currentSlide = 0;
        user.setUserType(AppConstants.UserType.VIEWER);
        groupReceiver.interrupt();
    }

    public void leaveSession() throws UnknownHostException, SocketException {
        globalSession.sendMessage(new Message(LEAVE_SESSION, new Participant(
                        user.getUsername(), user.getID(), Helpers.getInetAddress())),
                InetAddress.getByName(groupSession.getLeaderIPAddress()));
        cleanUpSessionData();
    }

    public synchronized void sendHelloMessage() throws IOException {
        System.out.println("Sending hello message.");
        globalSession.multicast(new Message(HELLO));

        if (helloCounter < PORT_TIMELINESS) {
            helloCounter++;
        }
    }

    public void multicastSlides() throws IOException {
        for (int i = 0; i < slides.size(); i++) {
            System.out.println("Sending slide " + i);
            for (var packet : PacketCreator.createPackets(slides.get(i), i)) {
                slidesSender.multicast(packet, SLIDES_MULTICAST_BASE_PORT + groupSession.getPort());
            }
        }
        multicastCurrentSlideNumber();
    }

    public void multicastCurrentSlideNumber() throws IOException {
        groupSession.sendGroupMessage(new Message(CURRENT_SLIDE_NUMBER,
                currentSlide));
    }

    public void sendCurrentSlideNumber(InetAddress address) {
        globalSession.sendMessage(new Message(CURRENT_SLIDE_NUMBER, currentSlide), address);
    }

    public void sendPacketLostMessage(InetAddress address, Message message) {
        System.out.println(address.getHostAddress() + " " + message.intVariable);
        globalSession.sendMessage(message, address);
    }

    public void sendMissingMessage(InetAddress address, Message message) {
        globalSession.sendMessage(message, address, PACKET_LOSS_PORT);
    }

    public void sendMissingSlide(InetAddress address, byte[] data) throws IOException {
        globalSession.sendMessage(data, address, PACKET_LOSS_PORT);
    }

    public void sendSlides(InetAddress senderAddress) throws IOException {
        slidesSender.unicast(slides, senderAddress);
        sendCurrentSlideNumber(senderAddress);
    }

    public void multicastSessionDetails() throws IOException {
        globalSession.multicast(new Message(SESSION_DETAILS, groupSession));
    }

    public void sendSessionDetails(InetAddress senderAddress) {
        while (!groupSessions.contains(groupSession)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("Leader info not ready.");
            }
        }
        globalSession.sendMessage(new Message(SESSION_DETAILS, groupSession), senderAddress);
    }

    public void sendDeleteSession() {
        stopCrashDetection();
        for (var participant : participants) {
            globalSession.sendMessage(new Message(DELETE_SESSION, groupSession), participant.ipAddress);
        }
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

    public void closeSession() {
        Platform.runLater(() -> {
            try {
                pvc.closeSession();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
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

    public synchronized void addParticipant(Participant participant) {
        participants.add(participant);
        Platform.runLater(() -> participantsNames.add(participant));
    }

    public synchronized void deleteParticipant(Participant participant) {
        participants.remove(participant);
        Platform.runLater(() -> participantsNames.remove(participant));
    }

    public synchronized void addAvailabilityConfirmedParticipant(Participant participant) {
        availabilityConfirmedParticipants.add(participant);
    }

    public void multicastDeleteParticipant(Participant participant) throws IOException {
        groupSession.sendGroupMessage(new Message(DELETE_PARTICIPANT, participant));
    }

    private CircularFifoQueue<Boolean> getCircularFifoQueue() {
        CircularFifoQueue<Boolean> queue = new CircularFifoQueue<>(PORT_TIMELINESS);
        for (int i = 0; i < 3; i++) {
            queue.add(false);
        }
        return queue;
    }

    public synchronized void addSessionData(GroupSession session) {
        groupSessions.add(session);

        if (portAvailabilityHistory.containsKey(session.getPort())) {
            portAvailabilityHistory.get(session.getPort()).add(true);
        } else {
            portAvailabilityHistory.put(session.getPort(), getCircularFifoQueue());
        }
        Platform.runLater(() -> groupSessionsInfo.add(session.getName() + ": " + session.getLeaderInfo()));
        System.out.println(session.getName());
    }

    public synchronized void cleanUpSessionsData() {
        groupSessions.clear();
        Platform.runLater(groupSessionsInfo::clear);
    }

    public synchronized void updateSessionData(GroupSession session, Participant leader, boolean afterCrash) throws UnknownHostException {
        if (leader.name.equals(user.getUsername())) {
            user.setUserType(AppConstants.UserType.PRESENTER);
            if (!afterCrash) {
                addParticipant(new Participant(groupSession.getLeaderName(),
                        groupSession.getLeaderID(), InetAddress.getByName(groupSession.getLeaderIPAddress())));
            }
        } else {
            user.setUserType(AppConstants.UserType.VIEWER);
        }

        groupSessions.get(groupSessions.indexOf(session)).setLeaderData(leader);
        if (groupSession.equals(session)) {
            groupSession.setLeaderData(leader);
            deleteParticipant(leader);
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

    public GroupSession getGroupSession(String name) {
        return groupSessions.stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
    }

    public GroupSession getGroupSession() {
        return groupSession;
    }

    public ObservableList<Participant> getParticipantsNames() {
        return participantsNames;
    }

    public List<Participant> getParticipants() {
        return participants;
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

    public void sendElect() {
        for (var participant : participants) {
            if (participant.ID < user.getID()) {
                globalSession.sendMessage(new Message(ELECT, user.getID()), participant.ipAddress);
            }
        }
    }

    public void sendCOORD() throws IOException {
        groupSession.sendGroupMessage(new Message(COORD,
                groupSession, new Participant(user.getUsername(), user.getID(), Helpers.getInetAddress())));
    }

    public void sendChangeLeader() throws IOException {
        globalSession.multicast(new Message(CHANGE_LEADER,
                groupSession, new Participant(user.getUsername(), user.getID(), Helpers.getInetAddress())));
    }

    public void sendStopElection(InetAddress senderAddress) {
        globalSession.sendMessage(new Message(STOP_ELECT), senderAddress);
    }

    public void stopElection() {
        crashDetection.stopElection();
    }

    public synchronized void agreeOnSlidesSender(InetAddress senderAddress) {
        if (!agreementMessageSent.containsKey(senderAddress) || !agreementMessageSent.get(senderAddress)) {
            int minID = participants.isEmpty() ? user.getID() :
                    participants.stream().min(Comparator.comparing(v -> v.ID)).get().ID;

            for (var participant : participants) {
                globalSession.sendMessage(new Message(START_AGREEMENT_PROCESS,
                        minID, senderAddress), participant.ipAddress);
            }
            agreementMessageSent.put(senderAddress, true);

            agreementTimer.put(senderAddress, new Timer(true));
            agreementTimer.get(senderAddress).schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (user.getID() == minID) {
                            sendSlides(senderAddress);
                        }
                        agreementMessageSent.put(senderAddress, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, AGREEMENT_PROCESS_TIMEOUT);
        }
    }

    public void sendStopAgreementProcess(InetAddress senderAddress, InetAddress viewerAddress) {
        globalSession.sendMessage(new Message(STOP_AGREEMENT_PROCESS, viewerAddress), senderAddress);
    }

    public synchronized void stopAgreementProcess(InetAddress viewerAddress) {
        agreementTimer.get(viewerAddress).cancel();
        agreementTimer.remove(viewerAddress);
        agreementMessageSent.remove(viewerAddress);
    }

    public void stopCrashDetection() {
        try {
            System.out.println("Stopped crash detection");
            crashDetection.stopCrashDetectionTimer();
            crashDetection.interrupt();
        } catch (Exception e) {
            System.out.println("Crash detection already stopped");
        }
        lastImAlive = 0;
    }

    public void startCrashDetection() {
        crashDetection = new CrashDetection();
        crashDetection.start();
    }

    public void sendImAlive() throws IOException {
        groupSession.sendGroupMessage(new Message(IM_ALIVE));
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

    public int getCurrentLeaderID() {
        return this.groupSession.getLeaderID();
    }

    public String getCurrentLeaderIP() {
        return this.groupSession.getLeaderIPAddress();
    }

    public void multicastNewParticipant(Participant participant) throws IOException {
        groupSession.sendGroupMessage(new Message(NEW_PARTICIPANT, participant));
    }

    public void handleMessage(Message message) throws IOException {
        this.packetHandler.handlePacket(message);
    }

    public PacketHandler getPacketHandler() {
        return this.packetHandler;
    }

    public void checkParticipantsAvailability() throws InterruptedException {
        availabilityConfirmedParticipants.clear();
        for (var participant : participants) {
            globalSession.sendMessage(new Message(CHECK_AVAILABILITY, groupSession), participant.ipAddress);
        }

        int counter = 0;
        boolean allAvailable = false;
        while (counter < CHECK_AVAILABILITY_TIMEOUT / CHECK_AVAILABILITY_INTERVAL) {
            Thread.sleep(CHECK_AVAILABILITY_INTERVAL);
            if (availabilityConfirmedParticipants.size() == participants.size()) {
                allAvailable = true;
                break;
            }
            counter++;
        }

        if (!allAvailable) {
            for (var participant : participants) {
                if (!availabilityConfirmedParticipants.contains(participant)) {
                    deleteParticipant(participant);
                    try {
                        multicastDeleteParticipant(participant);
                    } catch (IOException e) {
                        System.out.println("Failed to multicast delete participant");
                    }
                }
            }
        }
    }

    public void confirmAvailability(InetAddress senderAddress) {
        globalSession.sendMessage(new Message(CONFIRM_AVAILABILITY), senderAddress);
    }

    public void requestSlides(InetAddress senderAddress) {
        globalSession.sendMessage(new Message(REQUEST_SLIDES), senderAddress);
    }
}
