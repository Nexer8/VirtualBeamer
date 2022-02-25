package com.virtual.beamer.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Helpers {
    public static NetworkInterface getNetworkInterface() throws SocketException {
        NetworkInterface networkInterface = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        boolean found = false;
        while (interfaces.hasMoreElements() && !found)
        {
            NetworkInterface networkInterfaceTemp = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterfaceTemp.getInetAddresses();
            System.out.println(networkInterfaceTemp.getName());
            while(inetAddresses.hasMoreElements())
            {
                String address = inetAddresses.nextElement().getHostAddress();
                if(address.startsWith("192.168."))
                {
                    networkInterface = NetworkInterface.getByName(address);
                    found = true;
                    break;
                }
            }
        }
        return networkInterface;
    }
}
