package ro.upb.cs.direchat.Model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Clasa contine device-ul care ruleaza aplicatia
 */
public class LocalP2PDevice {

    private WifiP2pDevice localDevice;
    private static final LocalP2PDevice instance = new LocalP2PDevice();

    public WifiP2pDevice getLocalDevice() { return localDevice; }

    public void setLocalDevice(WifiP2pDevice localDevice) { this.localDevice = localDevice; }

    /**
     * Intoarce instanta clasei
     * @return instanta clasei
     */
    public static LocalP2PDevice getInstance() { return instance; }

    /**
     * Constructor privat
     */
    public LocalP2PDevice() { localDevice = new WifiP2pDevice(); }

}
