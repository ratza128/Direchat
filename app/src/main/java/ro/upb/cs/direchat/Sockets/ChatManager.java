package ro.upb.cs.direchat.Sockets;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import ro.upb.cs.direchat.Configuration;

/**
 * Clasa ce se ocupa de scriere/citire mesaje prin socket.
 * Foloseste un Handler pentrua trimite mesaje catre GUI (Ex UI Thread Android)
 * Aceasta clasa este folosita de {@link ro.upb.cs.direchat.Sockets.ClientSocketHandler}
 * si {@link ro.upb.cs.direchat.Sockets.GroupOwnerSocketHandler}
 */
public class ChatManager implements Runnable {

    private static final String TAG = "ChatHandler";

    private Socket socket = null;
    private final Handler handler;

    private boolean disable = false;
    private InputStream iStream;
    private OutputStream oStream;

    /**
     * Constructorul clasei
     */
    public ChatManager(@NonNull Socket socket, @NonNull Handler handler){
        this.socket = socket;
        this.handler = handler;
    }

    public boolean isDisable() {return disable;}
    public void setDisable(boolean disable) { this.disable = disable;}

    /**
     * Metoda ce executa thread-ul {@link ro.upb.cs.direchat.Sockets.ChatManager}
     * Pentru a opri executia, ".setDisable(true"
     */
    @Override
    public void run() {
        Log.d(TAG, "ChatManager started");
        try {
            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;

            //Apelul aceste metode este folosit pentru a apela handleMessage Configuration.FIRSTMESSAGEEXCHANGE in MainActivity
            handler.obtainMessage(Configuration.FIRSTMESSAGEXCHANGE, this).sendToTarget();
            while (!disable){
                try{
                    //Incearca sa citeasca din InputStream
                    if (iStream != null){
                        bytes = iStream.read(buffer);
                        if (bytes == -1 )
                            break;

                        //aceasta metoda
                        handler.obtainMessage(Configuration.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e){
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e){
            Log.e(TAG, "Exception: " + e.toString());
        } finally {
            try {
                iStream.close();
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception during close socket or isStream", e);
            }
        }
    }

    /**
     * Metoda ce scrie array de bytes (care poate sa fie mesaj) pentru un stream de output
     * @param buffer byte[] Array ce reprezinta data pe care o scriem.
     *               Un string convertit la byte[] este ".getBytes()"
     */
    public void write(byte[] buffer){
        try{
            oStream.write(buffer);
        } catch (IOException e){
            Log.e(TAG, "Exception during write", e);
        }
    }
}
