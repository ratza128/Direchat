package ro.upb.cs.direchat.Sockets;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

import ro.upb.cs.direchat.Configuration;

/**
 * Clasa ce implementeaza handler-ul ServerSocketului
 * Este folosit doar de GroupOwner
 */
public class GroupOwnerSocketHandler extends Thread {

    private static final String TAG = "GroupOwnerSocketHandler";

    private ServerSocket socket = null;
    private Handler handler;

    InetAddress ipAddress;

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    /**
     * Constructorul clasei
     * @param handler  Handler-ul pentru a putea comunica
     * @throws IOException Exceptie data de {@link ServerSocket} (SERVERPORT)
     */
    public GroupOwnerSocketHandler(@NonNull Handler handler) throws IOException {
        try {
            socket = new ServerSocket(Configuration.GROUPOWNER_PORT);
            this.handler = handler;
            Log.d(TAG, "Socket Started");
        } catch (IOException e){
            Log.e(TAG, "IOException during open ServerSockets with port" + Configuration.GROUPOWNER_PORT, e);
            pool.shutdownNow();
            throw e;
        }
    }

    /**
     * ThreadPool pentru socketii clientilor
     */
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            Configuration.THREAD_COUNT, Configuration.THREAD_COUNT,
            Configuration.THREAD_POOL_EXECUTOR_KEEP_ALIVE_TIME, TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>());

    /**
     * Metoda ce inchide socketul GroupOwnerului si inchide threadul
     */
    public void closeSocketAndKillThisThread() {
        if (socket != null && !socket.isClosed()){
            try {
                socket.close();
            } catch (IOException e){
                Log.e(TAG, "IOException during close Socket", e);
            }
            pool.shutdown();
        }
    }

    /**
     * Metoda ce porneste GroupOwnerSocketHandler
     * Nu se poate opri aceasta metoda( while(true) )
     */
    @Override
    public void run() {
        while (true) {
            try{
                //Se initializeaza o instanta ChatManager atunci cand este o conexiune noua
                if (socket != null && !socket.isClosed()) {
                    Socket clientSocket = socket.accept(); //deoarece acum sunt conectat cu un device client/peer
                    pool.execute(new ChatManager(clientSocket, handler));
                    ipAddress = clientSocket.getInetAddress();
                    Log.d(TAG, "Launching the I/O handler");
                }
            } catch (IOException e) {
                try{
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "IOException during close Socket", ioe);
                }
                pool.shutdownNow();
                break;
            }
        }
    }
}
