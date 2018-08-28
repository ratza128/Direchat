package ro.upb.cs.direchat.ActionListeners;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

/**
 * Contextul nu poate sa fie null
 * tag == null, clasa alege default tag "ActionListenerTag"
 * daca successToast == null , Toast-ul din onSuccess nu o sa fie afisat
 */
public class CustomizableActionListener implements WifiP2pManager.ActionListener {

    private final Context context;
    private final String successLog, successToast, failLog, failToast, tag;

    /**
     * Constructor CustomizableActionListener
     * successLog, successToast, failLog, failToast poate sa fie null (actiunea asociata este omisa)
     * @param context   Contextul necesar pentru a putea afisa Toast-urile
     * @param tag       String care reprezinta tag-ul Log.d (daca este null atunci se pune default "ActionListenerTag"
     * @param successLog    String care reprezinta mesajul pentru Log.d in onSuccess
     * @param successToast  String care reprezinta mesajul pentru toast in onSuccess
     * @param failLog       String care repezinta mesajul pentru Log.d in onFailure
     * @param failToast     String care reprezinta mesajul pentru toast in onFailure
     */
    public CustomizableActionListener(@NonNull Context context, String tag, String successLog, String successToast, String failLog, String failToast) {

        this.context = context;
        this.successLog = successLog;
        this.successToast = successToast;
        this.failLog = failLog;
        this.failToast = failToast;

        if (tag == null){
            this.tag = "ActionListenerTag";
        } else {
            this.tag = tag;
        }
    }

    @Override
    public void onSuccess() {
        if (successLog != null) {
            Log.d(tag, successLog);
        }
        if (context != null && successToast != null) {
            Toast.makeText(context, successToast, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFailure(int reason) {
        if (failLog != null)
            Log.d(tag, failLog + ", reason: " + reason);

        if (context != null && failToast != null)
            Toast.makeText(context, failToast, Toast.LENGTH_SHORT).show();
    }
}
