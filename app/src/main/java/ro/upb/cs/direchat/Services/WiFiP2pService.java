package ro.upb.cs.direchat.Services;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Structura ce retine informatiile serviciului
 */
public class WiFiP2pService {

    private WifiP2pDevice device;
    private String instanceName = null;
    private String serviceRegistrationType = null;

    /** Getter device **/
    public WifiP2pDevice getDevice(){
        return device;
    }

    /** Setter device **/
    public void setDevice(WifiP2pDevice device){
        this.device = device;
    }

    /** Getter InstanceName **/
    public String getInstanceName() {
        return instanceName;
    }

    /** Setter InstanceName **/
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /** Getter ServiceRegistrationType **/
    public String getServiceRegistrationType() {
        return serviceRegistrationType;
    }

    /** Setter ServiceRegistrationType **/
    public void setServiceRegistrationType(String serviceRegistrationType) {
        this.serviceRegistrationType = serviceRegistrationType;
    }
}
