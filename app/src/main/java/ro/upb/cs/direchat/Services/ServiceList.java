package ro.upb.cs.direchat.Services;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Clasa ce reprezinta o lista de {@link ro.upb.cs.direchat.Services.WiFiP2pService}
 * In aceasta lista se regasesc toate elementele gasite in urma cautarii in wifi
 *
 */
public class ServiceList {

    private final List<WiFiP2pService> serviceList;
    private static final ServiceList instance = new ServiceList();

    /**
     * Metoda ge intoarce instanta clasei
     */
    public static ServiceList getInstance() {return instance;}

    /**
     * Constructor private pentru ca este o clasa singleton
     */
    public ServiceList() {serviceList = new ArrayList<>(); }

    /**
     * Intoarce numarul de elemente din lista;
     * @return int care reprezinta numarul de elemente
     */
    public int getSize() { return serviceList.size(); }

    /**
     * Metoda ce sterge toate elementele din lista
     */
    public void clear() { serviceList.clear(); }


    /**
     * Metoda ce adauga un serviciu in lista clasei doar in cazul in care
     * acesta nu se afla deja in lista
     * @param service
     */
    public void addServiceIfNotPresent(WiFiP2pService service){
        boolean add = true;

        if (service == null)
            return;

        for (WiFiP2pService element : serviceList) {
            if (element.getDevice().equals(service.getDevice())
                    && element.getInstanceName().equals(service.getInstanceName()))
                add = false;
        }

        if (add)
            serviceList.add(service);
    }

    /**
     * Metoda ce face cautare in lista de servicii dupa un WifiP2pDevice si intoarce serviciul {@link ro.upb.cs.direchat.Services.WiFiP2pService}
     * Aceasta metoda foloseste doar deviceAddress deoarece, cateodata, android nu gaseste numele doar adresa mac
     * @param device WifiP2pDevice pe care il cautam
     * @return WiFiP2pService asociat deviceului sau null in cazul in care nu se gaseste in lista
     */
    public WiFiP2pService getServiceByDevice(WifiP2pDevice device){
        if (device == null)
            return null;

        for (WiFiP2pService element : serviceList){
            if (element.getDevice().deviceAddress.equals(device.deviceAddress))
                return element;
        }
        return null;
    }

    /**
     *  Metoda ce intoarce elemetul de la pozitia trimisa ca parametru (de la 0 la size)
     * @param position int Pozitia elementului pe care il vrem
     * @return Intoarce un element {@link WiFiP2pService} de la pozitia position trimisa ca parametru. In cazul in care
     */
    public WiFiP2pService getElementByPosition(int position) {
        if(position < 0 || position >= getSize())
            return null;

        return serviceList.get(position);
    }

}
