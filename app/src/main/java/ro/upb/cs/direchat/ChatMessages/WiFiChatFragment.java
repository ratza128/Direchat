package ro.upb.cs.direchat.ChatMessages;

import android.support.v4.app.Fragment;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ro.upb.cs.direchat.ChatMessages.WaitingToSend.WaitingToSendQueue;
import ro.upb.cs.direchat.DestinationDeviceTabList;
import ro.upb.cs.direchat.R;
import ro.upb.cs.direchat.Services.ServiceList;
import ro.upb.cs.direchat.Services.WiFiP2pService;
import ro.upb.cs.direchat.Sockets.ChatManager;

/**
 * Clasa ce prelucreaza UI chat-ului
 * include un listView de mesage si un camp de mesaj cu buton de send
 */
public class WiFiChatFragment extends Fragment {
    private static final String TAG = "WiFiChatFragment";

    private Integer tabNumber;
    private static boolean firstStartSendAddress;
    private boolean grayScale = true;
    private final List<String> items = new ArrayList<>();
    private TextView chatLine;
    private ChatManager chatManager;
    private WiFiChatMessageListAdapter adapter = null;


    public List<String> getItems() { return items; }

    public Integer getTabNumer() {
        return tabNumber;
    }

    public void setTabNumber (Integer tabNumber) {
        this.tabNumber = tabNumber;
    }

    public static boolean isFirstStartSendAddress () { return firstStartSendAddress; }

    public static void setFirstStartSendAddress ( boolean firstStartSendAddress ){
        WiFiChatFragment.firstStartSendAddress = firstStartSendAddress;
    }

    public boolean isGrayScale (){return grayScale;}

    public void setGrayScale(boolean grayScale) { this.grayScale = grayScale;}

    public ChatManager getChatManager() {
        return chatManager;
    }
    public void setChatManager(ChatManager chatManager) {this.chatManager = chatManager;}

    /**
     * Interfata chemata din {@link ro.upb.cs.direchat.MainActivity}
     * MainActivity implementeaza aceasta interfata
     */
    public interface AutomaticReconnectListener {
        public void reconnectToService(WiFiP2pService wiFiP2pService);
    }

    /**
     * Metoda ce intoarce instanta unui fragment nou
     * @return instanta fragmentului
     */
    public static WiFiChatFragment newInstance() {return new WiFiChatFragment(); }

    public WiFiChatFragment() {}

    /**
     * Metoda ce combina toate mesajele in
     * {@link ro.upb.cs.direchat.ChatMessages.WaitingToSend.WaitingToSendQueue}
     * intr-un string si il trimite catre
     * {@link ro.upb.cs.direchat.Sockets.ChatManager}
     * pentru a trimite mesajul
     */
    public void sendForcedWaitingToSendQueue() {
        Log.d(TAG, "sendForcedWaitingToSendQueue() called");

        String combinedMessages = "";
        List<String> listCopy = WaitingToSendQueue.getInstace().getWaitingToSendItemsList(tabNumber);
        for(String message : listCopy) {
            if (!message.equals("") && message.equals("\n")) {
                combinedMessages = combinedMessages + "\n" + message;
            }
        }

        combinedMessages = combinedMessages + "\n";
        Log.d(TAG, "Queued message to send: " + combinedMessages);
        if (chatManager != null){
            if (!chatManager.isDisable()) {
                chatManager.write((combinedMessages).getBytes());
                WaitingToSendQueue.getInstace().getWaitingToSendItemsList(tabNumber).clear();
            } else
            {
                Log.d(TAG, "ChatManager disabled, imposible to send the queued combined message");
            }
        }
    }

    /**
     * Metoda ce adauga mesajul in listView fragmentului si notifica de acest update la
     * {@link ro.upb.cs.direchat.ChatMessages.WiFiChatMessageListAdapter}
     * @param readMessage Mesajul pe care il trimitem
     */
    public void pushMessage(String readMessage) {
        items.add(readMessage);
        adapter.notifyDataSetChanged();
    }

    /**
     * Metoda ce updateaza {@link ro.upb.cs.direchat.ChatMessages.WiFiChatMessageListAdapter}
     */
    public void updateChatMessageListAdapter() {
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    /**
     * Metoda ce adauga textul din (EditText) chatLine in
     * {@link ro.upb.cs.direchat.ChatMessages.WaitingToSend.WaitingToSendQueue}
     * si incearca sa sa reconecteze la serviciul asociat deviceului din tab (cu indexul tabNumber)
     */
    public void addToWaitingToSendQueueAndTryReconnect() {
        WaitingToSendQueue.getInstace().getWaitingToSendItemsList(tabNumber).add(chatLine.getText().toString());

        WifiP2pDevice device = DestinationDeviceTabList.getInstance().getDevice(tabNumber - 1);
        if (device != null) {
            WiFiP2pService service = ServiceList.getInstance().getServiceByDevice(device);
            Log.d(TAG, "device address: " + device.deviceAddress + ", service: " + service);

            //apeleaza reconnectToService in MainActivity
            ((AutomaticReconnectListener) getActivity()).reconnectToService(service);
        } else{
            Log.d(TAG, "addToWaitingToSendQueueAndTryReconnect device == null, i can't do anything");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chatmessage_list, container, false);

        chatLine = (TextView) view.findViewById(R.id.txtChatLine);
        ListView listView = (ListView) view.findViewById(R.id.list);

        adapter = new WiFiChatMessageListAdapter(getActivity(),R.id.txtChatLine, this);
        listView.setAdapter(adapter);

        view.findViewById(R.id.sendMessage).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(chatManager != null){
                            if (!chatManager.isDisable()){
                                Log.d(TAG, "chatManager state: enable");
                                chatManager.write(chatLine.getText().toString().getBytes());
                            } else {
                                Log.d(TAG, "ChatManager disable, trying to send a message with tabNum= " + tabNumber);
                                addToWaitingToSendQueueAndTryReconnect();
                            }

                            pushMessage("Me: " + chatLine.getText().toString());
                            chatLine.setText("");
                        } else{
                            Log.d(TAG, "ChatManager is null");
                        }
                    }
                }

        );

        return view;
    }
}
