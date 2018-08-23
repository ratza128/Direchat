package ro.upb.cs.direchat.Services;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import ro.upb.cs.direchat.MainActivity;
import ro.upb.cs.direchat.Model.LocalP2PDevice;
import ro.upb.cs.direchat.Services.LocalDeviceGUI.LocalDeviceDialogFragment;


/**
 * Fragment ce contine recycleView cu lista WiFiP2pService si cardView-ul cu informatiile local device-ului
 * Daca se apasa pe un device, conexiunea incepe cu device-ul folosind ItemClickListener
 * Daca se apasa pe cardView deviceului local, un {@link ro.upb.cs.direchat.Services.LocalDeviceGUI.LocalDeviceDialogFragment}.
 * Aici poti modifica numele device-ului local si cu ajutorul DialogCallbackInterface obtinem datele introduse in dialogFragment
 */
public class WiFiP2pServicesFragment extends Fragment implements WiFiServicesAdapter.ItemClickListener, LocalDeviceDialogFragment.DialogConfirmListener{

    private static final String TAG = "WiFiP2pServiceFragment";

    private RecyclerView mRecyclerView;
    private WiFiServicesAdapter mAdapter;
    private TextView localDeviceNameText;


    /**
     * Interfata callback pentru a chema metode tryToConnectToAService in {@link ro.upb.cs.direchat.MainActivity}
     * MainActivity implementeaza aceasta interfata
     */
    public interface DeviceClickListener {
        public void tryToConnectToAService(int position);
    }

    /** Getter WiFiServiceAdapter **/
    public WiFiServicesAdapter getmAdapter() {
         return mAdapter;
    }

    /**
     * Metoda ce intoarce instanta unui nou fragment
     */
    public static WiFiP2pServicesFragment newInstance() { return new WiFiP2pServicesFragment(); }

    /**
     * Constructor default
     */
    public WiFiP2pServicesFragment () {}

    /**
     * Metoda care schimba numele device-ului local si face update in GUI
     * @param deviceName Noul nume al device-ului
     */
    @Override
    public void changeLocalDeviceName(String deviceName) {
        if (deviceName == null)
            return;

        localDeviceNameText.setText(deviceName);
        ((MainActivity)getActivity()).setDeviceNameWithReflection(deviceName);
    }

    /**
     * Metoda chemata din {@link ro.upb.cs.direchat.Services.WiFiServicesAdapter}
     * cu interfata {@link ro.upb.cs.direchat.Services.WiFiServicesAdapter.ItemClickListener}
     * cand user face click pe un element din RecyclerView
     */
     @Override
     public void itemClicked(View view){
        int clickedPosition = mRecyclerView.getChildPosition(view);

        //verificare
        if (clickedPosition >= 0)
            ((DeviceClickListener)getActivity()).tryToConnectToAService(clickedPosition);
     }

    /**
     * Metoda ce afiseaza pictograma group ownerului in card view-ul device-ului local
     */
    public void showLocalDeviceGoIcon(){
         if (getView() != null && getView().findViewById(R.id.groupOwnerLogo) != null && getView().findViewById(R.id.i_am_a_go_textview) != null){
             ImageView groupOwnerLogo = (ImageView) getView().findViewById(R.id.groupOwnerLogo);
             TextView i_am_a_go_textview = (TextView) getView().findViewById(R.id.i_am_a_go_textview);

             groupOwnerLogo.setImageDrawable(getResources().getDrawable(android.R.drawable.go_logo));
             groupOwnerLogo.setVisibility(View.VISIBLE);
             i_am_a_go_textview.setVisibility(View.VISIBLE);
         }
     }

    /**
     * Metoda ce sterge adresa ip in cardviewul device-ului local si inlocuieste
     * cu un mesaj de warning "Ip is not available"
     */
    public void resetLocalDeviceIpAddress() {
        if (getView() != null && getView().findViewById(R.id.localDeviceIpAddress) != null){
            TextView ipAddress = (TextView) getView().findViewById(R.id.localDeviceIpAddress);
        }
    }

    /**
     * Method care seteaza adresa ip a deviceului local in CARDVIEW
     * @param ipAddress string care reprezinta adresa ip de setat
     */
    public void setLocalDeviceIpAddress(String ipAddress){
        if (getView() != null && getView().findViewById(R.id.localDeviceIpAddress) != null){
            TextView ipAddressTextView = (TextView) getView().findViewById(R.id.localDeviceIpAddress);
            ipAddressTextView.setText(ipAddress);
        }
    }

    /**
     * Metoda ce ascunda pictograma de Group Owner in card view-ul device-ului local
     * Eficienta sa ascunda pictograma la intalnirea unui event precum disconnect
     */
    public void hideLocalDeviceGroupOwnerIcon(){
        if (getView() != null && getView().findViewById(R.id.go_logo) != null && getView().findViewById(R.id.i_am_a_go_textview) != null){
            ImageView groupOwnerLogoImageView = (ImageView) getView().findViewById(R.id.go_logo);
            TextView i_am_a_go_textview = (TextView) getView().findViewById(R.id.i_am_a_go_textview);

            groupOwnerLogoImageView.setVisibility(View.INVISIBLE);
            i_am_a_go_textview.setVisibility(View.INVISIBLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(android.R.layout.services_list,container, false);
        rootView.setTag(TAG);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(layoutManager);

        //permite optimizare daca toate elementele au aceeasi dimensiune
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new WiFiServicesAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        localDeviceNameText = (TextView) rootView.findViewById(R.id.localDeviceName);
        localDeviceNameText.setText(LocalP2PDevice.getInstance().getLocalDevice().deviceName);

        TextView localDeviceAddressText = (TextView) rootView.findViewById(R.id.localDeviceAddress);
        localDeviceAddressText.setText(LocalP2PDevice.getInstance().getLocalDevice().deviceAddress);

        CardView cardView = (CardView) rootView.findViewById(R.id.cardViewLocalDevice);
        cardView.setOnClickListener(new OnClickListenerLocalDevice(this));

        return rootView;
    }

    /**
     * Clasa interna ce implementeaza OnClickListener pentru cardViewul device
     * Este util pentru a deschide {@link ro.upb.cs.direchat.Services.LocalDeviceGUI.LocalDeviceDialogFragment}
     * dupa actiunea de click
     */
    class OnClickListenerLocalDevice implements View.OnClickListener {
        private final Fragment fragment;

        public OnClickListenerLocalDevice(Fragment fragment1) { fragment = fragment1;}

        @Override
        public void onClick(View v) {
            LocalDeviceDialogFragment localDeviceDialogFragment = (LocalDeviceDialogFragment) getFragmentManager().findFragmentByTag("localDeviceDialogFragment");

            if (localDeviceDialogFragment == null){
                localDeviceDialogFragment = LocalDeviceDialogFragment.newInstance();
                localDeviceDialogFragment.setTargetFragment(fragment, 0);

                localDeviceDialogFragment.show(getFragmentManager(), "localDeviceDialogFragment");
                getFragmentManager().executePendingTransactions();
            }
        }
    }
}
