package com.virtualbeamer.constants;

public class SessionConstants {
    public static final String GROUP_ADDRESS = "224.0.0.224";
    public static final int MULTICAST_PORT = 2244;
    public static final int SLIDES_MULTICAST_BASE_PORT = 3000;
    public static final int INDIVIDUAL_SLIDES_PORT = 2246;
    public static final int INDIVIDUAL_MESSAGE_PORT = 5555;
    public static final int PACKET_LOSS_PORT = 5558;
    public static final int SLIDE_PACKET_LOSS_PORT = 5559;
    public static final int STARTING_GROUP_PORT = 10000;
    public static final int LEADER_ELECTION_TIMEOUT = 500;
    public static final int AGREEMENT_PROCESS_TIMEOUT = 500;
    public static final int UNICAST_SEND_USER_DATA_PORT = 5556;
    public static final int CHECK_PARTICIPANT_AVAILABILITY_PORT = 5557;
    public static final int SO_TIMEOUT = 1000;
    public static final int HELLO_MESSAGE_PERIODICITY = 5;
    public static final int IM_ALIVE_PERIODICITY = 1500;
    public static final int CRASH_DETECTION_TIMEOUT = 10 * IM_ALIVE_PERIODICITY;
    public static final int MESSAGE_QUEUE_FLUSH = 2000;
    public static final int PORT_TIMELINESS = 3;
    public static final int CHECK_AVAILABILITY_TIMEOUT = 2000;
    public static final int SLIDES_AVAILABILITY_TIMEOUT = 5000;
    public static final int SLIDES_AVAILABILITY_INTERVAL = 200;
    public static final int COLLECT_USERS_DATA_TIMEOUT = 2000;
}
