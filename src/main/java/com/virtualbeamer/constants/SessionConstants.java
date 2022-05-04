package com.virtualbeamer.constants;

public class SessionConstants {
    public static final String GROUP_ADDRESS = "224.0.0.224";
    public static final int MULTICAST_PORT = 2244;
    public static final int SLIDES_MULTICAST_BASE_PORT = 3000;
    public static final int INDIVIDUAL_SLIDES_PORT = 2246;
    public static final int INDIVIDUAL_MESSAGE_PORT = 5555;
    public static final int STARTING_GROUP_PORT = 10000;
    public static final int LEADER_ELECTION_TIMEOUT = 500;
    public static final int AGREEMENT_PROCESS_TIMEOUT = 500;
    public static final int UNICAST_SEND_USER_DATA_PORT = 5556;
    public static final int UNICAST_COLLECT_PORTS_PORT = 5557;
    public static final int UNICAST_IM_ALIVE_PORT = 5558;
    public static final int CRASH_DETECTION_LOWER_BOUND_TIMEOUT = 500;
    public static final int SO_TIMEOUT = 1000;
    public static final int PERIODICITY_OF_HELLO_MESSAGE = 5;
    public static final int CRASH_DETECTION_TIMEOUT = 3000;
}
