package com.virtualbeamer.models;

import static com.virtualbeamer.utils.PacketCreator.SLIDE_PACKET_MAX_SIZE;

public class SlideReceiverData {
    public int currentSession = -1;
    public int slicesStored = 0;
    public int[] slicesCol = null;
    public byte[] imageData = null;
    public boolean sessionAvailable = false;
    public byte[] buffer = new byte[SLIDE_PACKET_MAX_SIZE + 8];
}
