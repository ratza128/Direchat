package ro.upb.cs.direchat;

import android.net.wifi.p2p.WifiP2pDevice;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import ro.upb.cs.direchat.Model.P2pDestinationDevice;

/**
 * Clasa ce reprezinta lista tab-urilor cu
 * {@link ro.upb.cs.direchat.ChatMessages.WiFiChatFragment}
 * Fiecare device din aceasta lista este "destination device" asociat cu tabFragments ;ceea ce inseamna ca fiecare mesaj scris
 * se trimite catre "destination device"
 *
 * Aceasta clasa
 *
 */
public class DestinationDeviceTabList {

    private final List<P2pDestinationDevice> destinationDeviceList;

    private static final DestinationDeviceTabList instance = new DestinationDeviceTabList();

    /**
     * Metoda ce intoarce instanta clasei
     */
    public static DestinationDeviceTabList getInstance() { return instance; }

    /**
     * constructor private
     */
    private DestinationDeviceTabList() { destinationDeviceList = new ArrayList<>(); }

    /**
     * Metoda ge intoarce WifiP2pDevice specificand pozitia elementului in lista.
     *
     */
    public WifiP2pDevice getDevice(int pos){
        if (pos >= 0 && pos <= destinationDeviceList.size() - 1) {
            return destinationDeviceList.get(pos).getP2pDevice();
        }else{
            return null;
        }
    }

    /**
     * Metoda ce seteaza un
     * {@link ro.upb.cs.direchat.Model.P2pDestinationDevice} la pozitia pos
     * in lista {@Link deviceList}.
     * Aceasta metoda inlocuieste elemtul de la pozitia {@code pos}, daca acest ele
     * @param pos
     * @param device
     */
    public void setDevice(int pos, @NonNull P2pDestinationDevice device) {
        if (pos >= 0 && pos <= destinationDeviceList.size() - 1)
            destinationDeviceList.set(pos, device);
        else
            destinationDeviceList.add(pos, device);
    }
}
