package ro.upb.cs.direchat.Services.LocalDeviceGUI;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import ro.upb.cs.direchat.R;


/**
 * Clasa ce reprezinta DialogFragmentul.
 * Schimba numele device-ului local astfel incat sa fie updatata GUI si
 * sa fie vazut de ceilalti cu numele nou.
 *
 */
public class LocalDeviceDialogFragment extends DialogFragment{

    private Button confirmButton;
    private EditText deviceNameEditText;


    /**
     * {@link ro.upb.cs.direchat.Services.WiFiP2pServicesFragment} implementeaza aceasta interfata;
     * Dar metoda de schimbare nume device este changeLocalDeviceName
     */
    public interface DialogConfirmListener {
        public void changeLocalDeviceName(String deviceName);
    }

    /**
     * Metoda ce intoarce instanta unui Fragment nou
     * @return instanta this.fragment
     */
    public static LocalDeviceDialogFragment newInstance(){
        return new LocalDeviceDialogFragment();
    }

    /**
     * Constructor default
     */
    public LocalDeviceDialogFragment() {}

    @Override
    public void onDestroyView(){
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.dialog, container, false);

        getDialog().setTitle("Choose your device name");
        deviceNameEditText = (EditText) v.findViewById(R.id.deviceNameEditText);
        confirmButton = (Button) v.findViewById(R.id.confirmButton);

        //adaugam listener care sa apeleze changeLocalDeviceName in WifiP2pServicesFragment dupa actiunea butonului confirmButton
        this.setListenerConfirm();

        return v;
    }

    /**
     * Dupa actiunea click asupra butonului confirmButton se modifica numele device-ului local
     *
     */
    private void setListenerConfirm() {
        confirmButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(deviceNameEditText.getText().toString() != "")
                    ((DialogConfirmListener)getTargetFragment()).changeLocalDeviceName(deviceNameEditText.getText().toString());
                dismiss();
            }
        });
    }

}
