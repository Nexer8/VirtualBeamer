package com.virtual.beamer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class User extends Thread {

    private LinkedList<Session> activeSession = new LinkedList<Session>();
    private Session loadedSession;

    public void loadSessions() {

        activeSession.clear();
        try (ServerSocket serverSocket = new ServerSocket(4321)) {
            Thread thread = new Thread() {
                public void run() {
                    try (Socket socket = new Socket("localhost", 1234)) {

                        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                        writer.println("GET");

                    } catch (UnknownHostException ex) {

                        System.out.println("Server not found: " + ex.getMessage());

                    } catch (IOException ex) {

                        System.out.println("[c]I/O error: " + ex.getMessage());
                    }
                }
            };
            thread.start();
            while (true) {
                Socket socket = serverSocket.accept();

                System.out.println("New client connected");

                InputStream inputStream = socket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                activeSession = (LinkedList<Session>) objectInputStream.readObject();
                return;
            }

        } catch (Exception ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void addSession(Session session) {
        try (Socket socket = new Socket("localhost", 1234)) {

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println("POST '" + session.getSessionName() + "' '" + session.getSessionIP() + "'");

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("[c]I/O error: " + ex.getMessage());
        }
    }

    public LinkedList<Session> getActiveSession() {
        return activeSession;
    }
}
