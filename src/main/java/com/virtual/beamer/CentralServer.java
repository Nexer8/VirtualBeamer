package com.virtual.beamer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

public class CentralServer extends Thread {

    private static LinkedList<Session> activeSessions;

    public CentralServer() throws UnknownHostException {
        activeSessions = new LinkedList<Session>();
        //activeSessions.add(new Session("test"));
        //activeSessions.add(new Session("test2"));
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {

            System.out.println("Server is listening on port " + 1234);

            while (true) {
                Socket socket = serverSocket.accept();

                System.out.println("New client connected");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String text = reader.readLine();
                System.out.println(text);

                if (text.equals("GET")) {
                    Thread thread = new Thread() {
                        public void run() {
                            try (Socket socket = new Socket("localhost", 4321)) {

                                OutputStream outputStream = socket.getOutputStream();
                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                                objectOutputStream.writeObject(activeSessions);

                            } catch (UnknownHostException ex) {

                                System.out.println("Server not found: " + ex.getMessage());

                            } catch (IOException ex) {

                                System.out.println("[s]I/O error: " + ex.getMessage());
                            }
                        }
                    };
                    thread.start();
                }
                if (text.split(" ")[0].equals("POST")) {
                    String[] values = text.split("'");
                    activeSessions.add(new Session(values[1], values[3]));
                    System.out.println(values[1] + " " + values[3]);
                }

            }

        } catch (Exception ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


}
