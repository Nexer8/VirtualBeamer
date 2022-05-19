package com.virtualbeamer.constants;

public enum MessageType {
    DELETE_SESSION,
    HELLO,
    NEXT_SLIDE,
    PREVIOUS_SLIDE,
    SESSION_DETAILS,
    COLLECT_PORTS,
    SEND_SESSION_PORT,
    JOIN_SESSION,
    SEND_USER_DATA,
    LEAVE_SESSION,
    ELECT,
    COORD,
    STOP_ELECT,
    START_AGREEMENT_PROCESS,
    STOP_AGREEMENT_PROCESS,
    IM_ALIVE,
    CURRENT_SLIDE_NUMBER,
    NACK_PACKET,
    CHANGE_LEADER
}
