package com.virtualbeamer.models;

import static com.virtualbeamer.utils.PacketCreator.MAX_PACKET_SIZE;

public class SlidesReceiverData {
    public int currentSession = -1;
    public int slicesStored = 0;
    public int[] slicesCol = null;
    public byte[] imageData = null;
    public boolean sessionAvailable = false;
    public byte[] buffer = new byte[MAX_PACKET_SIZE + 8];
}
