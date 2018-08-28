package ro.upb.cs.direchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.channels.Channel;
import java.util.HashMap;
import java.util.Map;

import ro.upb.cs.direchat.ActionListeners.CustomDnsSdTxtRecordListener;
import ro.upb.cs.direchat.ActionListeners.CustomDnsServiceResponseListener;
import ro.upb.cs.direchat.ActionListeners.CustomizableActionListener;
import ro.upb.cs.direchat.ChatMessages.MessageFilter.MessageException;
import ro.upb.cs.direchat.ChatMessages.MessageFilter.MessageFilter;
import ro.upb.cs.direchat.ChatMessages.WaitingToSend.WaitingToSendQueue;
import ro.upb.cs.direchat.ChatMessages.WiFiChatFragment;
import ro.upb.cs.direchat.Model.LocalP2PDevice;
import ro.upb.cs.direchat.Model.P2pDestinationDevice;
import ro.upb.cs.direchat.Services.ServiceList;
import ro.upb.cs.direchat.Services.WiFiP2pService;
import ro.upb.cs.direchat.Services.WiFiP2pServicesFragment;
import ro.upb.cs.direchat.Services.WiFiServicesAdapter;
import ro.upb.cs.direchat.Sockets.ChatManager;
import ro.upb.cs.direchat.Sockets.ClientSocketHandler;
import ro.upb.cs.direchat.Sockets.GroupOwnerSocketHandler;
import sun.applet.Main;

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

    /**
     * Aceasta metoda schimba culorile mesajelor in
     * {@link ro.upb.cs.direchat.ChatMessages.WiFiChatFragment}
     * @param grayScale bool cand este true sterge toate culorile din
     *                  {@link ro.upb.cs.direchat.ChatMessages.WiFiChatFragment}
     *                  bazat pe valoarea tabNum pentru a selecta corect tabul in
     *                  {@link ro.upb.cs.direchat.TabFragment}
     */
    public void addColorActiveTabs(boolean grayScale) {
        Log.d(TAG, "addColorActiveTabs() called, tabNum= " + tabNum);

        if (tabFragment.isValidTabNum(tabNum) && tabFragment.getChatFragmentByTab(tabNum) != null){
            tabFragment.getChatFragmentByTab(tabNum).setGrayScale(grayScale);
            tabFragment.getChatFragmentByTab(tabNum).updateChatMessageListAdapter();
        }
    }

    /**
     * Metoda seteaza numele {@link ro.upb.cs.direchat.Model.LocalP2PDevice}
     * in UI si in device. In acest fel, toate device-urile pot vedea schimbarea in
     * procesul de descoperire.
     * @param deviceName    String care reprezinta device-ul vizibil, in timpul descoperirii
     */
    public void setDeviceNameWithReflection(String deviceName) {
        try {
            Method m = manager.getClass().getMethod(
                    "setDeviceName",
                    new Class[]{WifiP2pManager.Channel.class, String.class,
                    WifiP2pManager.ActionListener.class});

            m.invoke(manager, channel, deviceName,
                    new CustomizableActionListener(
                            MainActivity.this,
                            "setDeviceNameWithReflection",
                            "Device name changed",
                            "Device name changed",
                            "Error, device name not changed",
                            "Error, device name not changed"
                    ));
        } catch (Exception e) {
            Log.e(TAG, "Exception during setDeviceNameWithReflection", e);
            Toast.makeText(MainActivity.this, "Impossible to change the device name",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Metoda ce seteaza {@link android.support.v7.widget.Toolbar}
     * ca supportActionBar in {@link android.support.v7.app.AppCompatActivity}
     */
    private void setupToolBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getResources().getString(R.string.app_name));
            toolbar.setTitleTextColor(Color.WHITE);
            toolbar.inflateMenu(R.menu.action_items);
            this.setSupportActionBar(toolbar);
        }
    }

    /**
     * Metoda chemeata automat de android
     * @param wifiP2pInfo
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        //Cu ajutorul server socketului , group ownerul accepta conexiuni si apoi
        //trimite cate un client socket pentru fiecare client. Acest lucru este facut de
        // {GroupOwnerSocketHandler}
        if (wifiP2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                Log.d(TAG,"socketHandler != null? = " +(socketHandler != null));
                socketHandler = new GroupOwnerSocketHandler(this.getHandler());
                socketHandler.start();

                //setam adresa ip a group ownerului
                TabFragment.getWiFiP2pServicesFragment().setLocalDeviceIpAddress(wifiP2pInfo.groupOwnerAddress.getHostAddress());

                //daca acest device est groupOwner, setam groupOwner ImagineView in cardView (in WiFiP2pServiceFragment)
                TabFragment.getWiFiP2pServicesFragment().showLocalDeviceGoIcon();
            } catch (IOException e) {
                Log.e(TAG, "Failed to create a server thread - " + e);
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            socketHandler = new ClientSocketHandler(this.getHandler(), wifiP2pInfo.groupOwnerAddress);
            socketHandler.start();

            //daca este GO atunci setam ImagineView-ul cardViewului in WiFiP2pServicesFragment
            TabFragment.getWiFiP2pServicesFragment().hideLocalDeviceGroupOwnerIcon();
        }

        Log.d(TAG, "onConnectionInfoAvailable setTabFragmentToPage with tabNum == " + tabNum);

        this.setTabFragmentToPage(tabNum);
    }

    /**
     * Metoda chemata automat de Android atunci cand
     * {@link ro.upb.cs.direchat.Sockets.ChatManager} cheama handler.obtainMessage(**).sendToTarget()
     */
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "handleMessage, tabNum in this activity is: " + tabNum);

        switch (msg.what) {
            //chemata de fiecare device la inceputul fiecarei conexiuni(noua sau oprita si redeschisa)
            case Configuration.FIRSTMESSAGEXCHANGE:
                final Object obj = msg.obj;
                Log.d(TAG, "HandleMessage, " + Configuration.FIRSTMESSAGEXCHANGE_MSG + "case");
                chatManager = (ChatManager) obj;

                sendAddress(LocalP2PDevice.getInstance().getLocalDevice().deviceAddress,
                        LocalP2PDevice.getInstance().getLocalDevice().deviceName,
                        chatManager);
                break;
            case Configuration.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;

                Log.d(TAG, "handleMessage, " +Configuration.MESSAGE_READ_MSG + " case");
                //construim un string cu bytes din buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, "Message: " + readMessage);

                //message filter usage
                try {
                    MessageFilter.getInstance().isFiltered(readMessage);
                } catch (MessageException e){
                    if (e.getReason() == MessageException.Reason.NULLMESSAGE) {
                        Log.d(TAG, "handleMessage, filter activated because the message is null = " + readMessage);
                        return true;
                    } else {
                        if (e.getReason() == MessageException.Reason.MESSAGETOSHORT) {
                            Log.d(TAG, "handleMessage, filter activated because the message is to short = " + readMessage);
                            return true;
                        } else {
                            if (e.getReason() == MessageException.Reason.MESSAGEBLACKLISTED) {
                                Log.d(TAG, "handleMessage, filter activated because the message contains blacklisted words. Message = " + readMessage);
                                return true;
                            }
                        }
                    }
                }

                //daca mesajul primit contine Configuration.MAGICADDRESSKEYWORD inseamna ca
                //acel device incearca sa se conecteze la acest device
                if (readMessage.contains(Configuration.MAGICADDRESSKEYWORD)) {
                    WifiP2pDevice p2pDevice = new WifiP2pDevice();
                    p2pDevice.deviceAddress = readMessage.split("___")[1];
                    p2pDevice.deviceName = readMessage.split("___")[2];
                    P2pDestinationDevice device = new P2pDestinationDevice(p2pDevice);

                    if (readMessage.split("___").length == 3) {
                        Log.d(TAG, "handleMessage, p2pDevice created with: " + p2pDevice.deviceName+ ", " + p2pDevice.deviceAddress);
                        manageAddressMessageReception(device);
                    } else if (readMessage.split("___").length == 4) {
                        device.setDestinationIpAddress(readMessage.split("___")[3]);

                        //setam adresa ip a clientului
                        TabFragment.getWiFiP2pServicesFragment().setLocalDeviceIpAddress(device.getDestinationIpAddress());
                        Log.d(TAG, "handleMessage, p2pDevice created with: " + p2pDevice.deviceName + ", " + p2pDevice.deviceAddress + ", " + device.getDestinationIpAddress());
                        manageAddressMessageReception(device);
                    }
                }

                //verific daca tabNum este valid ca sa fiu sigur
                if (tabFragment.isValidTabNum(tabNum)) {
                    if (Configuration.DEBUG_VERSION) {
                        //Folosesc pentru formatarea mesajului
                        //nu e chiar nevoie deoarece daca un mesaj contine MAGICADDRESSKEYWORD
                        //atunci mesajul trebuie sters sau folosit in logica
                        if (readMessage.contains(Configuration.MAGICADDRESSKEYWORD)) {
                            readMessage = readMessage.replace("+", "");
                            readMessage = readMessage.replace(Configuration.MAGICADDRESSKEYWORD, "Mac Address");
                        }

                        tabFragment.getChatFragmentByTab(tabNum).pushMessage("Buddy: " + readMessage);

                    } else {
                        if (!readMessage.contains(Configuration.MAGICADDRESSKEYWORD)) {
                            tabFragment.getChatFragmentByTab(tabNum).pushMessage("Buddy: " + readMessage);
                        }
                    }

                    //daca WaitingToSendQueue nu este gol , trimite toate mesajele catre device
                    if (!WaitingToSendQueue.getInstace().getWaitingToSendItemsList(tabNum).isEmpty()) {
                        tabFragment.getChatFragmentByTab(tabNum).sendForcedWaitingToSendQueue();
                    }
                } else {
                    Log.e("handleMessage", "Error tabNum = " + tabNum + "because is <= 0");
                }
                break;
        }
        return true;
    }

    private void manageAddressMessageReception(P2pDestinationDevice p2pDestinationDevice) {
        if (!DestinationDeviceTabList.getInstance().containsElement(p2pDestinationDevice)) {
            Log.d(TAG, "handleMessage, p2pDevice IS NOT in the DeviceTabList ");

            if (DestinationDeviceTabList.getInstance().getDevice(tabNum - 1) == null) {
                DestinationDeviceTabList.getInstance().setDevice(tabNum - 1, p2pDestinationDevice);

                Log.d(TAG, "handleMessage, p2pDevice in DeviceTabList at position tabNum = " + (tabNum - 1) + " is null");
            } else {
                DestinationDeviceTabList.getInstance().addDeviceIfRequired(p2pDestinationDevice);
                Log.d(TAG, "handleMessage, p2pDevice in DeviceTabList at position tabNum = " + (tabNum - 1) + " isn't null");
            }
        } else {
            Log.d(TAG, "handleMessage, p2pDevice IS already in the DeviceTabList");
        }

        //vreau sa fiu sigur ca trimit mesaj catre alt device cu LocalDevice macAddress
        //Inainte, trebuie sa selectez tabNum corect, este posibil ca acest tabNum sa fie incorect
        tabNum = DestinationDeviceTabList.getInstance().indexOfElement(p2pDestinationDevice) + 1;

        Log.d(TAG, "handleMessage, updated tabNum = " + tabNum );
        Log.d(TAG, "handleMessage, chatManager!=null?" + (chatManager != null));

        //daca chatManager != null primesc mesaj cu MAGICADDRESSKEYWORD de la alt device
        if (chatManager != null) {
            //adaug tab nou, daca este necesar(daca conversatia creata s-a oprit,
            //trebuie restart pentru a nu crea unul nou
            if (tabNum > TabFragment.getWiFiChatFragmentList().size()) {
                WiFiChatFragment fragment = WiFiChatFragment.newInstance();
                //adaugam un fragment nou, setam tabNum cu listSize + 1
                fragment.setTabNumber(TabFragment.getWiFiChatFragmentList().size() + 1);
                tabFragment.getmSectionAdapter().notifyDataSetChanged();
            }
            //update tabul afisat si culoarea
            this.setTabFragmentToPage(tabNum);
            this.addColorActiveTabs(false);
            Log.d(TAG, "tabNum is : " + tabNum);

            //setez chatManager , daca sunt in cazul Configuration.FIRSTMESSAGEEXCHANGE
            //se intampla atunci cand 2 device-uri incep sa se conecteze pentru prima data
            //sau dupa un event de disonect si GroupInfo este valabil
            tabFragment.getChatFragmentByTab(tabNum).setChatManager(chatManager);

            //pentru ca nu vreau sa reexecut codul de aici
            chatManager = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //quick fix pentru Android N
        StrictMode.ThreadPolicy policy = new
                StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.main);

        //activam wakeLock
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.setupToolBar();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        tabFragment = TabFragment.newInstance();

        this.getSupportFragmentManager().beginTransaction().replace(R.id.container_root, tabFragment, "tabFragment").commit();
        this.getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    protected void onRestart() {
        Fragment frag = getSupportFragmentManager().findFragmentById("services");
        if (frag != null) {
            getSupportFragmentManager().beginTransaction().remove(frag).commit();
        }

        TabFragment tabFragment = ((TabFragment)
        getSupportFragmentManager().findFragmentByTag("tabfragment"));
        if (tabFragment != null) {
            tabFragment.getmViewPager().setCurrentItem(0);
        }

        super.onRestart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discovery:
                ServiceList.getInstance().clear();

                if (discoveryStatus){
                    discoveryStatus = false;

                    item.setIcon(R.drawable.ic_action_search_stopped);
                    internalStopDiscovery();
                } else {
                    discoveryStatus = true;
                    item.setIcon(R.drawable.ic_action_search_searching);
                    startRegistration();
                    discoverService();
                }

                updateServiceAdapter();
                this.setTabFragmentToPage(0);
                return true;
            case R.id.disconnect:
                this.setTabFragmentToPage(0);
                this.forceDisconnectAndStartDiscovery();
                return true;
            case R.id.cancelConnection:
                this.setTabFragmentToPage(0);
                this.forcedCancelConnect();
                return  true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onResume() {
        super.onResume();
        receiver = new WiFiP2pBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onStop() {
        this.disconnectBecauseOnStop();
        super.onStop();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_items, menu);
        return true;
    }
}
