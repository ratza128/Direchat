package ro.upb.cs.direchat.Sockets;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import ro.upb.cs.direchat.Configuration;

/**
 * Clasa ce implementeaza handler-ul ClientSocketului.
 * Este folosit doar de clients/peers
 */
public class ClientSocketHandler extends Thread {

    private static final String TAG = "ClientSocketHandler";

    private final Handler handler;
    private final InetAddress mAddress; //adresa ip nu MAC
    private Socket socket;

    /**
     * Constructorul clasei
     * @param handler   Handler de care avem nevoie pentru a comunica
     * @param groupOwnerAddress Adresa ip a groupOwner al acestui Client/peer
     */
    public ClientSocketHandler(@NonNull Handler handler, @NonNull InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
    }

    /**
     * Metoda ce porneste
     * {@link ro.upb.cs.direchat.Sockets.ChatManager}
     */
    @Override
    public void run() {
        ChatManager chat;
        socket = new Socket();
        try{
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(), Configuration.GROUPOWNER_PORT), Configuration.CLIENT_PORT);
            Log.d(TAG, "Launching the I/O handler");
            chat = new ChatManager(socket,handler);
            new Thread(chat).start();
        } catch (IOException e) {
            Log.e(TAG, "IOException throwed by socket", e);
            try{
                socket.close();
            } catch (IOException e1){
                Log.e(TAG, "IOException during closing socket", e1);
            }
        }
    }

     public void closeSocketAndKillThisThread() {
        if (socket != null && !socket.isClosed()){
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException during closing socket", e);
            }
        }

        //pentru a intrerupe acest thread, fara threadPoolExecutor
         if (!this.isInterrupted()){
            Log.d(TAG, "Stopping ClientSocketHander");
            this.interrupt();
         }
     }
}
