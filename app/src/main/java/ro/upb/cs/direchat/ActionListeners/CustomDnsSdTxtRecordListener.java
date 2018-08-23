package ro.upb.cs.direchat.ActionListeners;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.Map;

import ro.upb.cs.direchat.Configuration;

/**
 *  Un DnsSdTxtRecordListener custom
 *  Nu este necesara.
 */
public class CustomDnsSdTxtRecordListener implements WifiP2pManager.DnsSdTxtRecordListener{
    private static final String TAG = "DnsSdRecordListener";

    @Override
    public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
        Log.d(TAG, "onDnsSdTxtRecordAvail: " + "device" + srcDevice.deviceName +" is " + txtRecordMap.get(Configuration.TXTRECORD_PROP_AVAILABLE));
    }
}
