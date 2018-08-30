package ro.upb.cs.direchat.ChatMessages;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import ro.upb.cs.direchat.R;

/**
 * Clasa este ArrayAdapter care reprezinta datele din
 * {@link ro.upb.cs.direchat.ChatMessages.WiFiChatFragment}
 * Aceasta clasa este accesata doar din pachetul ChatMessages
 */
public class WiFiChatMessageListAdapter extends ArrayAdapter<String>{
    private final WiFiChatFragment chatFragment;


    /**
     * Constructorul
     * @param context Contextul
     * @param textViewResourceId TextView id
     * @param chatFragment Se apeleaza din ChatFragment
     */
    public WiFiChatMessageListAdapter(Context context, int textViewResourceId, WiFiChatFragment chatFragment){
        super(context, textViewResourceId, chatFragment.getItems());
        this.chatFragment = chatFragment;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null)
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatmessage_row, parent, false);

        String message = chatFragment.getItems().get(position);
        if (message != null && !message.isEmpty()) {
            TextView nameText = (TextView) v.findViewById(R.id.message);
            if (nameText != null){
                nameText.setText(message);
                nameText.setTextAppearance(chatFragment.getActivity(), R.style.normalText);
                if (chatFragment.isGrayScale()) {
                    nameText.setTextColor(chatFragment.getResources().getColor(R.color.gray));
                } else {
                    if (message.startsWith("Me: ")) {
                        nameText.setTextAppearance(chatFragment.getActivity(), R.style.normalText);
                    } else {
                        nameText.setTextAppearance(chatFragment.getActivity(), R.style.boldText);
                    }
                }
            }
        }

        return v;
    }
}
