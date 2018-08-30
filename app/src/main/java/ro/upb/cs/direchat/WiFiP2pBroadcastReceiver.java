package ro.upb.cs.direchat;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import ro.upb.cs.direchat.Model.LocalP2PDevice;

/**
 * Un BroadcastReceiver care anunta event wifi p2p
 * Aceasta clasa functioneaza fara apel
 */
public class WiFiP2pBroadcastReceiver extends BroadcastReceiver{

    private static final String TAG = "P2pBroadcastReceiver";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final Activity activity;

    /**
     * Constructor
     * @param manager   System Service WifiP2pManager
     * @param channel   Wifi p2p Channel
     * @param activity  Activity-ul asociat receiver-ului
     */
    public WiFiP2pBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, Activity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG,action);

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){

            if (manager == null)
                return;

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                //suntem conectat cu celalalt device
                //cerem info pentru a afla Ip-ul ownerului(GroupOwner)
                Log.d(TAG, "Connected to p2p network. Requesting network details");

                manager.requestConnectionInfo(channel, (WifiP2pManager.ConnectionInfoListener) activity);

                ((MainActivity)activity).setConnected(true);

                //coloram tab-ul activ
                ((MainActivity)activity).addColorActiveTabs(false);

                //schimbam tab vizibil
                ((MainActivity)activity).setTabFragmentToPage(((MainActivity)activity).getTabNum());
            } else {
                //este deconectat, restartam procesul de descoperire device
                Log.d(TAG, "Disconnect. Restarting discovery process.");

                //scoate culoarea de la toate taburile
                ((MainActivity)activity).addColorActiveTabs(true);

                //daca manualItemMenuDisconnectAndStartDiscovery() nu este activat de user
                if (!((MainActivity)activity).isBlockForcedDiscoveryInBroadcastReceiver()) {
                    ((MainActivity) activity).forcedDiscoveryStop();
                    ((MainActivity) activity).restartDiscovery();
                }

                //Disable toateChatManager
                ((MainActivity)activity).setDisableAllChatManagers();

                //Punem primul tab vizibil (servicesList) pentru a putea vedea servicile la care ne putem conecta
                ((MainActivity)activity).setTabFragmentToPage(0);

                ((MainActivity)activity).setConnected(false);

                //stergere iconita GroupOwner din cardView
                TabFragment.getWiFiP2pServicesFragment().hideLocalDeviceGroupOwnerIcon();

                //stergem adresa ip din interiorul cardview-ului device local
                TabFragment.getWiFiP2pServicesFragment().resetLocalDeviceIpAddress();
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //daca nu este disconnect sau connect, setam deviceLocal
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            LocalP2PDevice.getInstance().setLocalDevice(device);
            Log.d(TAG, "Local Device status -" + device.status);
        }
    }
}
