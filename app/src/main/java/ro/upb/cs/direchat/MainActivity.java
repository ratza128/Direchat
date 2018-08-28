package ro.upb.cs.direchat;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.Toolbar;

import java.net.InetAddress;
import java.nio.channels.Channel;
import java.util.HashMap;
import java.util.Map;

import ro.upb.cs.direchat.ActionListeners.CustomDnsSdTxtRecordListener;
import ro.upb.cs.direchat.ActionListeners.CustomDnsServiceResponseListener;
import ro.upb.cs.direchat.ActionListeners.CustomizableActionListener;
import ro.upb.cs.direchat.ChatMessages.WiFiChatFragment;
import ro.upb.cs.direchat.Model.P2pDestinationDevice;
import ro.upb.cs.direchat.Services.ServiceList;
import ro.upb.cs.direchat.Services.WiFiP2pService;
import ro.upb.cs.direchat.Services.WiFiP2pServicesFragment;
import ro.upb.cs.direchat.Services.WiFiServicesAdapter;
import ro.upb.cs.direchat.Sockets.ChatManager;
import ro.upb.cs.direchat.Sockets.ClientSocketHandler;
import ro.upb.cs.direchat.Sockets.GroupOwnerSocketHandler;

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

    /**
     * Metoda ce face restart la procesul de descoperire si updateaza UI
     */
    public void restartDiscovery() {
        discoveryStatus = true;

        //incepe o noua
        this.startRegistration();
        this.discoverService();
        this.updateServiceAdapter();
    }

    /**
     * Metoda ce descopera serviciul si pune rezultatul
     * in {@link ro.upb.cs.direchat.Services.ServiceList}
     * Aceasta metoda schimba si elementul din meniu
     */
    private void discoverService() {

        ServiceList.getInstance().clear();

        toolbar.getMenu().findItem(R.id.discovery).setIcon(getResources().getDrawable(R.drawable.ic_action_search_searching));

        /*
         * Inregistreaza receptorii pentru serviciile DNS-SD. Acestea sunt calluri facute de sistem atunci cand un serviciu
         * este descoperit.
         */
        manager.setDnsSdResponseListeners(channel, new CustomDnsServiceResponseListener(), new CustomDnsSdTxtRecordListener());

        //Dupa ce am pus ascultatorii,se creaza un request de serviciu si se initializeaza descoperirea
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        //initializare descoperire
        manager.addServiceRequest(channel, serviceRequest, new CustomizableActionListener(
                MainActivity.this,
                "discoverService",
                "Added service",
                null,
                "Failed adding service discovery request",
                "Failed adding service discovery request"));

        //incepe descoperire servicii
        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated");
                Toast.makeText(MainActivity.this, "Service discovery initiated", Toast.LENGTH_SHORT).show();
                blockForcedDiscoveryInBroadcastReceiver = false;
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Service discovery failed");
                Toast.makeText(MainActivity.this, "Service discovery failed, " + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Metoda ce apeleaza notifyDataSetChanged pentru adaptorul
     * {@link ro.upb.cs.direchat.Services.WiFiP2pServicesFragment}
     */
    private void updateServiceAdapter() {
        WiFiP2pServicesFragment fragment = TabFragment.getWiFiP2pServicesFragment();
        if (fragment != null){
            WiFiServicesAdapter adapter = fragment.getmAdapter();
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    }

    /**
     * Metoda ce deconecteaza device-ul cand aceasta Activity cheama onStop()
     */
    private void disconnectBecauseOnStop() {

        this.closeAndKillSocketHandler();
        this.setDisableAllChatManagers();
        this.addColorActiveTabs(true);

        if (manager != null && channel != null){
            manager.removeGroup(channel,
                    new CustomizableActionListener(
                            MainActivity.this,
                            "disconnectBecauseOnStop",
                            "Disconnected",
                            "Disconnected",
                            "Disconnect failed",
                            "Disconnect failed"
                    ));
        } else {
            Log.d("disconnectBecauseOnStop", "Imposible to disconnect");
        }
    }

    /**
     * Metoda close&kill socketHandler
     * GroupOwner socket sau Client socket
     */
    private void closeAndKillSocketHandler() {
        if (socketHandler instanceof GroupOwnerSocketHandler) {
            ((GroupOwnerSocketHandler) socketHandler).closeSocketAndKillThisThread();
        } else{
            ((ClientSocketHandler) socketHandler).closeSocketAndKillThisThread();
        }
    }

    private void forceDisconnectAndStartDiscovery() {
        //Aceasta metoda este apelata de 2 ori atunci cand primeste notificare de deconectare
        //Din acest motiv folosesc blockForcedDiscoveryInBroadcastReceiver pentru a verifica daca
        //este nevoie sa apelez BroadcastReceiver
        this.blockForcedDiscoveryInBroadcastReceiver = true;
        this.closeAndKillSocketHandler();
        this.setDisableAllChatManagers();

        if (manager != null && channel != null) {
            manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Disconnected");
                    Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Discovery status: " + discoveryStatus);
                    forcedDiscoveryStop();
                    restartDiscovery();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Disonnected failed. Reason :" + reason);
                    Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.d(TAG, "Disconnect impossible");
        }
    }

    /**
     * Inregistreaza un seviciu local
     */
    private void startRegistration() {
        Map<String, String> record = new HashMap<>();
        record.put(Configuration.TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                Configuration.SERVICE_INSTANCE,
                Configuration.SERVICE_REG_TYPE,
                record
        );

        manager.addLocalService(channel, service, new CustomizableActionListener(
                MainActivity.this,
                "startRegistration",
                "Added Local Service",
                null,
                "Failed to add a service",
                "Failed to add a service"
        ));
    }

    private void connectP2p(WiFiP2pService service){
        Log.d(TAG, "connectP2p, tabNum before = " + tabNum);

        if (DestinationDeviceTabList.getInstance().containsElement(new P2pDestinationDevice(service.getDevice()))){
            this.tabNum = DestinationDeviceTabList.getInstance().indexOfElement(new P2pDestinationDevice(service.getDevice()));
        }

        if (this.tabNum == -1)
            Log.d("ERROR", "ERROR TABNUM = 1");

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.getDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0; //Vreau ca acest device sa fie client; uneori devine si GroupOwner chiar daca pun 0 aici

        if (serviceRequest != null) {
            manager.removeServiceRequest(channel, serviceRequest,
                    new CustomizableActionListener(
                            MainActivity.this,
                            "ConnectP2p",
                            null,
                            "RemoveServiceRequest success",
                            null,
                            "removeServiceRequest failed"
                    ));
        }

        manager.connect(channel, config,
                new CustomizableActionListener(
                        MainActivity.this,
                        "connectP2p",
                        null,
                        "Connecting to service",
                        null,
                        "Failed connecting to service"
                ));
    }

    /**
     * Metoda chemata de {@link ro.upb.cs.direchat.Services.WiFiP2pServicesFragment}
     * cu interfata {@link ro.upb.cs.direchat.Services.WiFiP2pServicesFragment.DeviceClickListener}
     * Atunci cand userul face click pe un element al recyclerView.
     * @param position int care reprezinta pozitia un care s-a facut click-ul
     *                 {@link ro.upb.cs.direchat.Services.WiFiP2pServicesFragment}
     */
    public void tryToConnectToAService(int position) {
        WiFiP2pService service = ServiceList.getInstance().getElementByPosition(position);

        //daca este conectat, deconectam si restartam procesul de descoperire
        if (connected)
            this.forceDisconnectAndStartDiscovery();

        //adaugam device in DeviceTabList doar daca este nevoie
        DestinationDeviceTabList.getInstance().addDeviceIfRequired(new P2pDestinationDevice(service.getDevice()));

        this.connectP2p(service);
    }

    /**
     * Metoda ce trimite {@link ro.upb.cs.direchat.Configuration}.MAGICADDRESSKEYWORD cu adresa mac a deviceului catre
     * alt device
     * @param deviceMacAddress String care reprezinta adresa mac a device-ului destinatar
     * @param name              String care reprezinta numele desvice-ului destinatar
     * @param chatManager
     */
    private void sendAddress(String deviceMacAddress, String name, ChatManager chatManager) {
        if (chatManager != null) {
            InetAddress ipAddress;
            if (socketHandler instanceof GroupOwnerSocketHandler) {
                ipAddress = ((GroupOwnerSocketHandler) socketHandler).getIpAddress();
                Log.d(TAG, "sending message with MAGICADDRESSKEYWORD with ipAddress= " + ipAddress.getHostAddress());

                chatManager.write((Configuration.PLUSSYMBOLS + Configuration.MAGICADDRESSKEYWORD +
                        "___" + deviceMacAddress + "___" + name + "___" + ipAddress.getHostAddress()).getBytes());
            } else {
                Log.d(TAG, "sending message with MAGICADDRESSKEYWORD without ipaddress");
                //folosesc simbolul "+" pentru a fi sigur ca atunci cand se pierd caractere intotdeauna o sa am
                //Configuration.MAGICADDRESSKEYWORD si pot sa asociez device-ul la WifiChatFragmentul corect
                chatManager.write((Configuration.PLUSSYMBOLS + Configuration.MAGICADDRESSKEYWORD +
                        "___" + deviceMacAddress + "___" + name).getBytes());
            }
        }
    }

    /**
     * Metoda care opreste toate {@link ro.upb.cs.direchat.Sockets.ChatManager}
     * Aceasta metoda face disable la toate ChatManagers
     */
    public void setDisableAllChatManagers() {
        for (WiFiChatFragment chatFragment : TabFragment.getWiFiChatFragmentList()){
            if (chatFragment != null && chatFragment.getChatManager() != null)
                chatFragment.getChatManager().setDisable(true);
        }
    }

    /**
     * Metoda care seteaza item-ul curent a {@link android.support.v4.view.ViewPager} folosit in
     * {@link ro.upb.cs.direchat.TabFragment}
     * @param numPage   int care reprezinta indexul tabului pe care vrem sa-l afisam
     */
    public void setTabFragmentToPage(int numPage) {
        TabFragment tabFragment = ((TabFragment) getSupportFragmentManager().findFragmentByTag("tabfragment"));
        if (tabFragment != null && tabFragment.getmViewPager() != null)
            tabFragment.getmViewPager().setCurrentItem(numPage);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
