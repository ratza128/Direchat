package ro.upb.cs.direchat.Model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Clasa
 */
public class P2pDestinationDevice {

    private WifiP2pDevice p2pDevice;
    private String destinationIpAddress; //ip address

    public WifiP2pDevice getP2pDevice() { return p2pDevice; }
    public String getDestinationIpAddress() { return destinationIpAddress; }

    public void setDestinationIpAddress(String destinationIpAddress){
        this.destinationIpAddress = destinationIpAddress;
    }

    /**
     * Constructorul clasei
     * @param p2pDevice {@code WifiP2pDevice}
     */
    public P2pDestinationDevice(WifiP2pDevice p2pDevice) { this.p2pDevice = p2pDevice; }

    /**
     * Alt constructor fara parametru
     */
    public P2pDestinationDevice() {}

    @Override
    public String toString() {
        return this.p2pDevice.deviceName + ", " + this.p2pDevice.deviceAddress + ", " + this.p2pDevice.status;
    }
}
