package com.virtual.beamer;

import java.net.UnknownHostException;

public class RuntimeTest {

    public static CentralServer centralServer;
    //public static VirtualBeamer virtualBeamer;

    public static void main(String[] args) throws UnknownHostException {
        centralServer = new CentralServer();
        centralServer.start();
        //virtualBeamer = new VirtualBeamer();
    }


}
