package com.virtualbeamer.services;

import com.virtualbeamer.constants.AppConstants;
import com.virtualbeamer.constants.SessionConstants;
import com.virtualbeamer.models.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import static com.virtualbeamer.constants.SessionConstants.SO_TIMEOUT;
import static com.virtualbeamer.utils.MessageHandler.deserializeMessage;
import static com.virtualbeamer.utils.MessageHandler.handleMessage;

public class CrashDetection extends Thread {
    private Timer crashDetectionTimer;
    private boolean electSent = false;
    private Timer electionTimer;

    public void stopElection() {
        electionTimer.cancel();
        electSent = false;
    }

    public void stopTimer() {
        crashDetectionTimer.cancel();
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
                        MainService.getInstance().setUserType(AppConstants.UserType.PRESENTER);
                        electSent = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, SessionConstants.LEADER_ELECTION_TIMEOUT);
        }
    }

    private void sendCrashDetectionCheck(int delay) {
        crashDetectionTimer = new Timer(false);
        System.out.println("Sending crash detection message in " + delay);

        crashDetectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    MainService.getInstance().sendCrashDetect();

                    boolean leaderCrashed = true;

                    try (DatagramSocket socket = new DatagramSocket(SessionConstants.UNICAST_IM_ALIVE_PORT)) {
                        socket.setSoTimeout(SO_TIMEOUT);
                        byte[] buffer = new byte[10000];
//                        TODO: try to replace with collectAndProcessMessage(socket, buffer);
                        //noinspection InfiniteLoopStatement
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

    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                int delay = SessionConstants.CRASH_DETECTION_LOWER_BOUND_TIMEOUT + (int) (Math.random() * 1000);
                sendCrashDetectionCheck(delay);
                // TODO: Sleep inside a thread is not good practice.
                sleep((long) delay + 1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
