package ro.upb.cs.direchat.ChatMessages.WaitingToSend;

import java.util.ArrayList;
import java.util.List;

/**
 * Clasa ce reprezinta elementul ArrayList-ului din lista din
 * {@link ro.upb.cs.direchat.ChatMessages.WaitingToSend.WaitingToSendQueue}
 */
public class WaitingToSendListElement {

    private final List<String> waitingToSendList;

    public List<String> getWaitingToSendList() { return waitingToSendList; }

    public WaitingToSendListElement () { waitingToSendList = new ArrayList<>(); }
}
