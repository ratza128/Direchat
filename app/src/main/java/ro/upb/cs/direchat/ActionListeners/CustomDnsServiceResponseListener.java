package ro.upb.cs.direchat.ActionListeners;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.Tag;
import android.util.Log;

import ro.upb.cs.direchat.Configuration;
import ro.upb.cs.direchat.Services.ServiceList;
import ro.upb.cs.direchat.Services.WiFiP2pService;
import ro.upb.cs.direchat.Services.WiFiP2pServicesFragment;
import ro.upb.cs.direchat.Services.WiFiServicesAdapter;
import ro.upb.cs.direchat.TabFragment;

public class CustomDnsServiceResponseListener implements WifiP2pManager.DnsSdServiceResponseListener {
    private static final String TAG = "DnsResponseListener";

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
        //Un serviciu a fost descoperit. Este serviciul aplicatiei ?
        if (instanceName.equalsIgnoreCase(Configuration.SERVICE_INSTANCE)){

            //updateaza UI si adauga deviceul descoperit
            WiFiP2pServicesFragment fragment = TabFragment.getWiFiP2pServicesFragment();
            if (fragment != null) {
                WiFiServicesAdapter adapter = fragment.getmAdapter();
                WiFiP2pService service = new WiFiP2pService();
                service.setDevice(srcDevice);
                service.setInstanceName(instanceName);
                service.setServiceRegistrationType(registrationType);

                ServiceList.getInstance().addServiceIfNotPresent(service);

                if (adapter != null)
                    adapter.notifyItemInserted(ServiceList.getInstance().getSize() - 1);

                Log.D(TAG, "onDnsSdServiceAvailable " + instanceName);
            }
            }
        }
    }
}
