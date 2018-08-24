package ro.upb.cs.direchat;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.Toolbar;

import java.nio.channels.Channel;

import ro.upb.cs.direchat.ChatMessages.WiFiChatFragment;
import ro.upb.cs.direchat.Model.P2pDestinationDevice;
import ro.upb.cs.direchat.Services.ServiceList;
import ro.upb.cs.direchat.Services.WiFiP2pService;
import ro.upb.cs.direchat.Services.WiFiP2pServicesFragment;
import ro.upb.cs.direchat.Sockets.ChatManager;

public class MainActivity extends AppCompatActivity implements
        WiFiP2pServicesFragment.DeviceClickListener,
        WiFiChatFragment.AutomaticReconnectListener,
        Handler.Callback,
        WifiP2pManager.ConnectionInfoListener{

    private static final String TAG = "MainActivity";
    private boolean retryChannel = false;

    private boolean connected = false;
    private int tabNum = 1;
    private boolean blockForcedDiscoveryInBroadcastReceiver = false;
    private boolean discoveryStatus = true;

    private TabFragment tabFragment;
    private Toolbar toolbar;
    private WifiP2pManager manager;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiP2pManager.Channel channel;

    private final IntentFilter intentFilter = new IntentFilter();
    private BroadcastReceiver receiver = null;
    private Thread socketHandler;
    private final Handler handler = new Handler(this);

    private ChatManager chatManager;

    Handler getHandler() { return handler; }

    int getTabNum() { return tabNum; }

    public boolean isBlockForcedDiscoveryInBroadcastReceiver() {
        return blockForcedDiscoveryInBroadcastReceiver;
    }

    public void setBlockForcedDiscoveryInBroadcastReceiver(boolean blockForcedDiscoveryInBroadcastReceiver) {
        this.blockForcedDiscoveryInBroadcastReceiver = blockForcedDiscoveryInBroadcastReceiver;
    }

    public TabFragment getTabFragment() {
        return tabFragment;
    }

    Toolbar getToolbar() { return toolbar; }

    public void setToolbar(Toolbar toolbar) { this.toolbar = toolbar;}

    public void setConnected(boolean connected) { this.connected = connected; }

    /**
     * Metoda chemata de WiFiChatFragment folosind interfata
     * {@link ro.upb.cs.direchat.ChatMessages.WiFiChatFragment.AutomaticReconnectListener} implementata de MainActivity.
     * Daca serviciul wifiP2pService este null, aceasta metoda returneaza direct fara sa faca ceva
     * @param wiFiP2pService
     */
    @Override
    public void reconnectToService(WiFiP2pService wiFiP2pService) {
        if (wiFiP2pService != null){
            Log.d(TAG, "reconnectToService called");

            //Adaugare device in DeviceTabList doar cand este cazul
            DestinationDeviceTabList.getInstance().addDeviceIfRequired(new P2pDestinationDevice(wiFiP2pService.getDevice()));
            this.connectP2p(wiFiP2pService);
        }
    }

    /**
     * Metoda ce opreste incercarea de a se conecta
     */
    private void forcedCancelConnect() {
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
             Log.d(TAG, "forcedCancelConnect success");
             Toast.makeText(MainActivity.this, "Cancel connect success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "forcedCancelConnect failed, reason: " + reason);
                Toast.makeText(MainActivity.this, "Cancel connect failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Metoda care forteaza oprirea descoperirii de dive nou
     * Sterge {@link ro.upb.cs.direchat.Services.ServiceList}
     * Face update la meniul de discovery si sterge toate serviciile inregistrare
     */
    public void forcedDiscoveryStop() {
        if (discoveryStatus) {
            discoveryStatus = false;

            ServiceList.getInstance().clear();
            //TODO TODO TODO iconita
            toolbar.getMenu().findItem(R.id.discovery).setIcon(getResources().getDrawable(R.drawable));

            this.internalStopDiscovery();
        }
    }

    /**
     * Metoda ce opreste procesul de discovery prin manager
     * {@link ro.upb.cs.direchat.Services.WifiP2pManager}
     */
    public void internalStopDiscovery() {
        manager.stopPeerDiscovery(channel,
                new CustomizableActionListener(
                        MainActivity.this,
                        "internalStopDiscovery",
                        "Discovery stopped",
                        "Discovery stopped",
                        "Discovery stop failed",
                        "Discovery stop failed"));
        manager.clearServiceRequests(channel,
                new CustomizableActionListener(
                        MainActivity.this,
                        "internalStopDiscovery",
                        "ClearServiceRequests success",
                        null,
                        "Discovery stop failed",
                        null));
        manager.clearLocalServices(channel,
                new CustomizableActionListener(
                        MainActivity.this,
                        "internalStopDiscovery",
                        "ClearLocalServices success",
                        null,
                        "clearLocalServices failure",
                        null));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
