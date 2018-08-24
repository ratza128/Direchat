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
     * Aceasta metoda inlocuieste elemtul de la pozitia {@code pos} doar in cazul in care deviceList are
     * element la pozitia respectiva. In caz contrar se adauga element la pozitia respectiva.
     *
     * @param pos int pozitia
     * @param device  {@link ro.upb.cs.direchat.Model.P2pDestinationDevice} elemntul pe care vrem sa-l adaugam in lista
     */
    public void setDevice(int pos, @NonNull P2pDestinationDevice device) {
        if (pos >= 0 && pos <= destinationDeviceList.size() - 1)
            destinationDeviceList.set(pos, device);
        else
            destinationDeviceList.add(pos, device);
    }

    /**
     * Metoda care adauga un {@link ro.upb.cs.direchat.Model.P2pDestinationDevice}
     * in lista (doar daca nu exista deja).
     * Daca lista contine elemente nule, aceasta metoda cauta primul element null si adauga in locul acestuia.
     * Daca nu exista elemente nule se adauga la sfarsitul listei;
     * @param device
     */
    public void addDeviceIfRequired(@NonNull P2pDestinationDevice device){
        boolean add = true;
        for (P2pDestinationDevice element : destinationDeviceList){
            if (element != null && element.getP2pDevice() != null && element.getP2pDevice().equals(device.getP2pDevice()))
                add = false; //deja exista
        }

        //trebuie sa adaug elementul
        if (add) {
            //caut primul element null si in inlocuiesc cu P2pDestination primit ca parametru
            for (int i = 0; i <= destinationDeviceList.size(); i++)
                if (destinationDeviceList.get(i) == null) {
                    destinationDeviceList.set(i, device);
                    return;
                }
            //daca lista nu are element null atunci adaug la sfarsitul listei
            destinationDeviceList.add(device);
        }
    }
}
