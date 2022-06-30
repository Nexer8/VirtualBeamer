# Bully Algorithm

1. When a process *P* notices that the coordinator crashed, it initiates the leader election process.
2. *P* sends an *ELECT* message, including its own ID, to all processes with lower IDs using **TCP**.
3. If no one responds, *P* wins and sends a *COORD* message to other processes using multicast. (*COORD* packet loss is handled by *Remark 1*).
4. If a process *P'* receives an *ELECT* message, it responds (stopping the former candidate) and starts a new election (if it has not started one alread).
5. Normally, if a process that was previously down, comes back up, it hosts a new election. In this case, however, the previous leader will return to be a new leader without starting a new election - existing leader will just **multicast** a *COORD*.
    - If there is anybody that didn't receive the *COORD* message due to a packet loss, it can quickly realize who the leader is by verifying the sender of *IM_ALIVE*.
    - Actually, *COORD* and *CHANGE_LEADER* are the same message. We can delete one. :question:

## Remarks

1. *SESSION_DETAILS*, which is sent as a response to periodical *HELLO* message should update existing sessions by looking at its port. This way, each process inside a session knows who the leader is, even if it didn't receive the *COORD*.
2. How do we manage eventual loss of a *DELETE_SESSION* message? It is sent globally.
    - We can delete sessions whose leaders didn't respond to *HELLO* message in a certain time (e.g. 3 times in a row, quite similar to crash detection). :question:
3. *COLLECT_PORTS* cannot be sent via multicast if we do not handle global packet losses.
    - We either implement a global packet loss handling or use TCP to send *COLLECT_PORTS* messages directly to the leaders (we have a list of available sessions, but we cannot be sure if it is complete).
    - Actually, we don't even need to use *COLLECT_PORTS* since *SESSION_DETAILS*, which is the answer to *HELLO* message, provides us with group session details including its port - we always have a list of all available *groupSessions*. :question:
    - Still, **how can we be sure(!) that there is no other group session with the same port?** :heavy_exclamation_mark: If we ignore an existing session, we'll simply override the leader...