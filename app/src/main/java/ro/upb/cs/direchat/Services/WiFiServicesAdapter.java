package ro.upb.cs.direchat.Services;

import android.net.wifi.p2p.WifiP2pDevice;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ro.upb.cs.direchat.R;

import org.w3c.dom.Text;

/**
 * Clasa WiFiServicseAdapter cu RecyclerView (Lollipop)si
 * {@link ro.upb.cs.direchat.Services.WiFiServicesAdapter.ViewHolder}
 */
public class WiFiServicesAdapter extends RecyclerView.Adapter<WiFiServicesAdapter.ViewHolder> {

    private final ItemClickListener itemClickListener;

    /**
     * Constructorul adapter-ului
     * @param itemClickListener
     */
    public WiFiServicesAdapter(@NonNull ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
        setHasStableIds(true);
    }

    public interface ItemClickListener {
        void itemClicked(final View view);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final View parent;
        private final TextView nameText;
        private final TextView statusText;
        private final TextView macAddressText;

        public ViewHolder(View view) {
            super(view);

            this.parent = view;
            nameText = (TextView) view.findViewById(R.id.message);
            macAddressText = (TextView) view.findViewById(R.id.text2);
            statusText = (TextView) view.findViewById(R.id.text3);
        }

        public void setOnClickListener(View.OnClickListener listener){
            parent.setOnClickListener(listener);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v = layoutInflater.inflate(R.layout.service_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        WiFiP2pService service = ServiceList.getInstance().getElementByPosition(position);
        if (service != null) {
            holder.nameText.setText(service.getDevice().deviceName + " - " + service.getInstanceName());
            holder.macAddressText.setText(service.getDevice().deviceAddress);
            holder.statusText.setText(getDeviceStatus(service.getDevice().status));
        }

        holder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.itemClicked(v);
            }
        });
    }
    @Override
    public int getItemCount() { return ServiceList.getInstance().getSize(); }

    /**
     * Metoda privata folosita in interiorul clasei pentru a obtine statusul din codul acestuia
     * @param statusCode    int care reprezinta condul statusului
     * @return  String ce reprezinta codul interpretat
     */
    private static String getDeviceStatus(int statusCode) {
        switch (statusCode) {
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                    return "Failed";
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unvailable";
            default:
                return "Unknown";
        }
    }
}
