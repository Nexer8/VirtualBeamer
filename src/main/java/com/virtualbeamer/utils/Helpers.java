package com.virtualbeamer.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class Helpers {
    public static NetworkInterface getNetworkInterface() throws SocketException {
        NetworkInterface networkInterface = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        boolean found = false;
        while (interfaces.hasMoreElements() && !found) {
            NetworkInterface networkInterfaceTemp = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterfaceTemp.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                String address = inetAddresses.nextElement().getHostAddress();
                if (address.startsWith("192.168.")) {
                    networkInterface = networkInterfaceTemp;
                    found = true;
                    break;
                }
            }
        }
        return networkInterface;
    }

    public static InetAddress getInetAddress() throws SocketException, UnknownHostException {
        NetworkInterface networkInterface = Helpers.getNetworkInterface();
        InetAddress inetAddress = null;

        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            String address = inetAddresses.nextElement().getHostAddress();
            if (address.startsWith("192.168.")) {
                inetAddress = InetAddress.getByName(address);
                break;
            }
        }
        return inetAddress;
    }
}
