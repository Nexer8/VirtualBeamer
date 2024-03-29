package com.virtualbeamer.services;

import com.virtualbeamer.constants.SessionConstants;
import com.virtualbeamer.models.Participant;
import com.virtualbeamer.utils.Helpers;

import java.io.IOException;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

import static com.virtualbeamer.constants.AppConstants.UserType.PRESENTER;
import static com.virtualbeamer.constants.SessionConstants.CRASH_DETECTION_TIMEOUT;
import static com.virtualbeamer.constants.SessionConstants.IM_ALIVE_PERIODICITY;

public class CrashDetection extends Thread {
    private Timer aliveMessageTimer;
    private boolean electSent = false;
    private Timer electionTimer;

    public void stopElection() {
        electionTimer.cancel();
        electSent = false;
    }

    public void stopCrashDetectionTimer() {
        aliveMessageTimer.cancel();
    }

    private void electLeader() throws IOException {
        if (!electSent) {
            MainService.getInstance().sendElect();
            electSent = true;

            electionTimer = new Timer(true);
            electionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        MainService.getInstance().sendCOORD();
                        MainService.getInstance().updatePreviousLeaderIP(MainService.getInstance().getCurrentLeaderIP());
                        MainService.getInstance().updateSessionData(MainService.getInstance().getGroupSession(),
                                new Participant(MainService.getInstance().getUsername(),
                                        MainService.getInstance().getID(), Helpers.getInetAddress()), true);
                        electSent = false;
                        MainService.getInstance().startCrashDetection();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, SessionConstants.LEADER_ELECTION_TIMEOUT);
        }
    }

    public void run() {
        try {
            if (MainService.getInstance().getUserType() == PRESENTER) {
                aliveMessageTimer = new Timer(false);
                aliveMessageTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            MainService.getInstance().sendImAlive();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 0, IM_ALIVE_PERIODICITY);
            } else {
                aliveMessageTimer = new Timer(false);
                aliveMessageTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            Instant instant = Instant.now();
                            System.out.println("Check im-alive: "
                                    + (instant.getEpochSecond() - MainService.getInstance().getLastImAlive()));
                            if (MainService.getInstance().getLastImAlive() != 0 && instant.getEpochSecond()
                                    - MainService.getInstance().getLastImAlive() > CRASH_DETECTION_TIMEOUT / 1000) {
                                MainService.getInstance().stopCrashDetection();
                                electLeader();
                                MainService.getInstance().startCrashDetection();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 0, CRASH_DETECTION_TIMEOUT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
